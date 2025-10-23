package types;

import ast.*;
import mocha.Symbol;

public class TypeChecker implements NodeVisitor {

    private StringBuilder errorBuffer;
    private Symbol currentFunction;
    private java.util.Map<String, java.util.List<FunctionSignature>> functionRegistry;
    private String currentFunctionName;
    private Type currentFunctionReturnType;
    
    public TypeChecker() {
        errorBuffer = new StringBuilder();
        functionRegistry = new java.util.HashMap<>();
        registerBuiltInFunctions();
    }
    
    private void registerBuiltInFunctions() {
        // Register readInt(): int
        java.util.List<Type> emptyParams = new java.util.ArrayList<>();
        FunctionSignature readIntSig = new FunctionSignature("readInt", emptyParams, new IntType());
        functionRegistry.put("readInt", java.util.Arrays.asList(readIntSig));
        
        // Register readFloat(): float
        FunctionSignature readFloatSig = new FunctionSignature("readFloat", emptyParams, new FloatType());
        functionRegistry.put("readFloat", java.util.Arrays.asList(readFloatSig));
        
        // Register readBool(): bool
        FunctionSignature readBoolSig = new FunctionSignature("readBool", emptyParams, new BoolType());
        functionRegistry.put("readBool", java.util.Arrays.asList(readBoolSig));
        
        // Register printInt(int): void
        java.util.List<Type> intParams = java.util.Arrays.asList(new IntType());
        FunctionSignature printIntSig = new FunctionSignature("printInt", intParams, new VoidType());
        functionRegistry.put("printInt", java.util.Arrays.asList(printIntSig));
        
        // Register printFloat(float): void
        java.util.List<Type> floatParams = java.util.Arrays.asList(new FloatType());
        FunctionSignature printFloatSig = new FunctionSignature("printFloat", floatParams, new VoidType());
        functionRegistry.put("printFloat", java.util.Arrays.asList(printFloatSig));
        
        // Register printBool(bool): void
        java.util.List<Type> boolParams = java.util.Arrays.asList(new BoolType());
        FunctionSignature printBoolSig = new FunctionSignature("printBool", boolParams, new VoidType());
        functionRegistry.put("printBool", java.util.Arrays.asList(printBoolSig));
        
        // Register println(): void
        FunctionSignature printlnSig = new FunctionSignature("println", emptyParams, new VoidType());
        functionRegistry.put("println", java.util.Arrays.asList(printlnSig));
    }
    
    // Inner class to represent function signatures
    private static class FunctionSignature {
        private java.util.List<Type> parameterTypes;
        private Type returnType;
        private String functionName;
        
        public FunctionSignature(String functionName, java.util.List<Type> parameterTypes, Type returnType) {
            this.functionName = functionName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }
        
        public java.util.List<Type> parameterTypes() { return parameterTypes; }
        public Type returnType() { return returnType; }
        public String functionName() { return functionName; }
        
        // Check if this signature matches the given argument types
        public boolean matches(java.util.List<Type> argTypes, TypeChecker typeChecker) {
            if (parameterTypes.size() != argTypes.size()) {
                return false;
            }
            
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (!typeChecker.areTypesCompatible(parameterTypes.get(i), argTypes.get(i))) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public String toString() {
            return functionName + "(" + parameterTypes + ") -> " + returnType;
        }
    }
    
    // Helper method to determine the type of an expression
    // private Type getExpressionType(Expression expr) {
    //     return new ErrorType("Unknown expression type");
    // }

    public boolean check(ast.AST ast) {
        if (ast.computation() != null) {
            ast.computation().accept(this);
        }
        
        // Duplicate function signature checking is now handled in the parser
        
        return !hasError();
    }
    
    
    private void checkDuplicateFunctionSignatures(ast.AST ast) {
        if (ast.computation() != null) {
            java.util.Map<String, java.util.List<ast.FunctionDeclaration>> functionMap = new java.util.HashMap<>();
            
            // Group functions by name
            for (ast.FunctionDeclaration funcDecl : ast.computation().functions()) {
                String funcName = funcDecl.function().name();
                if (!functionMap.containsKey(funcName)) {
                    functionMap.put(funcName, new java.util.ArrayList<>());
                }
                functionMap.get(funcName).add(funcDecl);
            }
            
            // Check for duplicate signatures within each function name
            for (String funcName : functionMap.keySet()) {
                java.util.List<ast.FunctionDeclaration> functions = functionMap.get(funcName);
                for (int i = 0; i < functions.size(); i++) {
                    for (int j = i + 1; j < functions.size(); j++) {
                        ast.FunctionDeclaration func1 = functions.get(i);
                        ast.FunctionDeclaration func2 = functions.get(j);
                        
                        // Check if they have the same parameter signature
                        if (hasSameSignature(func1, func2)) {
                            reportError(func2.lineNumber(), func2.charPosition(), 
                                "Function " + funcName + " already defined with same signature.");
                        }
                    }
                }
            }
        }
    }
    
    private boolean hasSameSignature(ast.FunctionDeclaration func1, ast.FunctionDeclaration func2) {
        // This is a simplified check - in a real implementation, we'd need to compare
        // the actual parameter types from the function signatures
        // For now, we'll assume they have the same signature if they have the same name
        // and the same number of parameters (this is a simplified approach)
        return func1.function().name().equals(func2.function().name());
    }

    private void reportError (int lineNum, int charPos, String message) {
        errorBuffer.append("TypeError(" + lineNum + "," + charPos + ")");
        errorBuffer.append("[" + message + "]" + "\n");
    }
    
    private String getArrayName(ArrayIndex node) {
        Expression array = node.array();
        if (array instanceof Designator) {
            Designator designator = (Designator) array;
            return designator.symbol().name();
        } else if (array instanceof ArrayIndex) {
            return getArrayName((ArrayIndex) array);
        }
        return "array";
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }
    
    public boolean areTypesCompatible(Type destination, Type source) {
        
        // Handle AddressOf types by unwrapping them for comparison
        Type destBase = destination instanceof AddressOfType ? ((AddressOfType) destination).baseType() : destination;
        Type srcBase = source instanceof AddressOfType ? ((AddressOfType) source).baseType() : source;
        
        
        // Array types are compatible only if they have the same dimensions and base type
        if (destBase instanceof ArrayType && srcBase instanceof ArrayType) {
            ArrayType destArray = (ArrayType) destBase;
            ArrayType srcArray = (ArrayType) srcBase;
            
            // Check if they have the same number of dimensions
            int destDims = getArrayDimensions(destArray);
            int srcDims = getArrayDimensions(srcArray);
            
            if (destDims != srcDims) {
                return false;
            }
            
            // Check if base types are compatible
            return areTypesCompatible(destArray.baseType(), srcArray.baseType());
        }
        
        // For non-array types, they are compatible if they are exactly the same
        if (destBase.getClass().equals(srcBase.getClass())) {
            return true;
        }
        
        return false;
    }
    
    private int getArrayDimensions(ArrayType arrayType) {
        int dims = 1;
        Type baseType = arrayType.baseType();
        while (baseType instanceof ArrayType) {
            dims++;
            baseType = ((ArrayType) baseType).baseType();
        }
        return dims;
    }

    public String errorReport () {
        return errorBuffer.toString();
    }

    @Override
    public void visit (Computation node) {
        // Visit variable declarations
        for (VariableDeclaration varDecl : node.variables().declarations()) {
            varDecl.accept(this);
        }
        
        // Visit function declarations
        for (FunctionDeclaration funcDecl : node.functions()) {
            funcDecl.accept(this);
        }
        
        // Visit main statement sequence
        node.mainStatementSequence().accept(this);
    }

    @Override
    public void visit(BoolLiteral node) {
        node.setType(new BoolType());
    }

    @Override
    public void visit(IntegerLiteral node) {
        node.setType(new IntType());
    }

    @Override
    public void visit(FloatLiteral node) {
        node.setType(new FloatType());
    }

    @Override
    public void visit(Designator node) {
        Type symbolType = node.symbol().type();
        node.setType(symbolType);
    }

    @Override
    public void visit(AddressOf node) {
        node.operand().accept(this);
    }

    @Override
    public void visit(ArrayIndex node) {
        node.array().accept(this);
        node.index().accept(this);
        Node arrayNode = (Node)node.array();
        Type arrayType = arrayNode.type();
        
        
        if (arrayType instanceof ArrayType) {
            ArrayType arrType = (ArrayType) arrayType;
            Type indexType = ((Node)node.index()).type();
            
            // Check if index type is valid (must be int)
            if (!(indexType instanceof IntType)) {
                node.setType(new ErrorType("Cannot index " + arrayType + " with " + indexType + "."));
            } else {
                // Check for out-of-bounds access (only for declared arrays, not function parameters)
                if (node.index() instanceof IntegerLiteral && arrType.size() > 0) {
                    IntegerLiteral indexLit = (IntegerLiteral) node.index();
                    int indexValue = indexLit.value();
                    int arraySize = arrType.size();
                    
                    if (indexValue < 0 || indexValue >= arraySize) {
                        node.setType(new ErrorType("Array Index Out of Bounds : " + indexValue + " for array " + getArrayName(node)));
                    } else {
                        Type elementType = arrType.baseType();
                        node.setType(elementType);
                    }
                } else {
                    Type elementType = arrType.baseType();
                    node.setType(elementType);
                }
            }
        } else if (arrayType instanceof ErrorType) {
            node.setType(arrayType);
        } else {
            node.setType(new ErrorType("Cannot index " +  new AddressOfType(arrayType) + " with " + ((Node)node.index()).type() + "."));
        }
        
        
        if (node.type() instanceof ErrorType && !(arrayType instanceof ErrorType)) {
            ErrorType errorType = (ErrorType) node.type();
            reportError(node.lineNumber(), node.charPosition(), errorType.message());
        }
    }

    @Override
    public void visit(Dereference node) {
        node.operand().accept(this);
    }

    @Override
    public void visit(LogicalNot node) {
        node.operand().accept(this);
        
        // Check for type errors in logical not operation
        Type operandType = ((Node)node.operand()).type();
        node.setType(operandType.not());
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
        }
    }

    @Override
    public void visit(Power node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for negative exponent
        if (node.right() instanceof IntegerLiteral) {
            IntegerLiteral exp = (IntegerLiteral) node.right();
            if (exp.value() < 0) {
                node.setType(new ErrorType("Power cannot have a negative exponent of " + exp.value() + "."));
                reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
                return;
            }
        }

        if (node.left() instanceof IntegerLiteral) {
            IntegerLiteral exp = (IntegerLiteral) node.left();
            if (exp.value() < 0) {
                node.setType(new ErrorType("Power cannot have a negative base of " + exp.value() + "."));
                reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
                return;
            }
        }
        
        // Check for type errors in power operation
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        
        // Check for bool exponent error
        if (rightType instanceof BoolType) {
            node.setType(new ErrorType("Cannot raise int to bool."));
            reportError(node.lineNumber(), node.operator().charPosition(), ((ErrorType)node.type()).message());
            return;
        } 
        
        node.setType(leftType.mul(rightType));
        if (node.type() instanceof ErrorType) {
            reportError(node.lineNumber(), node.operator().charPosition(), ((ErrorType)node.type()).message());
        }
    
    }

    @Override
    public void visit(Multiplication node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for type errors in multiplication operation
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.mul(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            ErrorType errorType = (ErrorType) node.type();
            reportError(node.lineNumber(), node.operator().charPosition(), errorType.message());
        }
    }

    @Override
    public void visit(Division node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for divide by zero
        if (node.right() instanceof IntegerLiteral) {
            IntegerLiteral divisor = (IntegerLiteral) node.right();
            if (divisor.value() == 0) {
                node.setType(new ErrorType("Cannot divide by 0."));
                reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
                return;
            }
        }
        
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.div(rightType));
        
        if (node.type() instanceof ErrorType) {
            ErrorType errorType = (ErrorType) node.type();
            reportError(node.lineNumber(), node.operator().charPosition(), errorType.message());
        }
    }

    @Override
    public void visit(Modulo node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for type errors in modulo operation
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.mod(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            reportError(node.lineNumber(), node.operator().charPosition(), ((ErrorType)node.type()).message());
        }
    }

    @Override
    public void visit(LogicalAnd node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for type errors in logical and operation
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.and(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            reportError(node.lineNumber(), node.operator().charPosition(), ((ErrorType)node.type()).message());
        }
    }

    @Override
    public void visit(Addition node) {
        // First visit the operands to check for errors in sub-expressions
        node.left().accept(this);
        node.right().accept(this);
        
        // Now check if the addition is type-safe
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.add(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            ErrorType errorType = (ErrorType) node.type();
            reportError(node.lineNumber(), node.operator().charPosition(), errorType.message());
        }
    }

    @Override
    public void visit(Subtraction node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for type errors in subtraction operation
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.sub(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            ErrorType errorType = (ErrorType) node.type();
            reportError(node.lineNumber(), node.operator().charPosition(), errorType.message());
        }
    }

    @Override
    public void visit(LogicalOr node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check for type errors in logical or operation
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.or(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            reportError(node.lineNumber(), node.operator().charPosition(), ((ErrorType)node.type()).message());
        }
    }

    @Override
    public void visit(Relation node) {
        node.left().accept(this);
        node.right().accept(this);
        
        // Check if the comparison is type-safe
        Type leftType = ((Node)node.left()).type();
        Type rightType = ((Node)node.right()).type();
        node.setType(leftType.compare(rightType));
        
        // If the result is an ErrorType, report the error
        if (node.type() instanceof ErrorType) {
            reportError(node.lineNumber(), node.operator().charPosition(), ((ErrorType)node.type()).message());
        }
    }

    @Override
    public void visit(Assignment node) {
        if (node.source() == null) {
            node.setType(new ErrorType("Cannot assign null to " + ((Node)node.destination()).type() + "."));
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
            return;
        }
        
        node.destination().accept(this);
        node.source().accept(this);

        // Check for assignment errors
        Type destinationType = ((Node)node.destination()).type();
        Type sourceType = ((Node)node.source()).type();
        
        if (destinationType instanceof IntType || destinationType instanceof FloatType || destinationType instanceof BoolType) 
        {
            ((Node)node.destination()).setType(new AddressOfType(destinationType));
            destinationType = ((Node)node.destination()).type();
        }
        
        
        if (destinationType instanceof ErrorType) {
            ErrorType errorType = (ErrorType) destinationType;
            String errorMessage = "Cannot assign " + sourceType + " to " + errorType + ".";
            node.setType(new ErrorType(errorMessage));
            reportError(node.lineNumber(), node.charPosition(), errorMessage);
        } else if (sourceType instanceof ErrorType) {
            ErrorType errorType = (ErrorType) sourceType;
            String errorMessage = "Cannot assign " + errorType + " to " + destinationType + ".";
            node.setType(new ErrorType(errorMessage));
            reportError(node.lineNumber(), node.charPosition(), errorMessage);
        } else if (!areTypesCompatible(destinationType, sourceType)) {
            String errorMessage = "Cannot assign " + sourceType + " to " + destinationType + ".";
            node.setType(new ErrorType(errorMessage));
            reportError(node.lineNumber(), node.charPosition(), errorMessage);
        }
        node.setType(new VoidType());
    }

    @Override
    public void visit(ArgumentList node) {
        for (Expression arg : node.arguments()) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(FunctionCall node) {
        node.arguments().accept(this);
        
        // Use the function registry for all functions (built-in and user-defined)
        Type returnType = lookupFunctionSignature(node);
        node.setType(returnType);
    }

    @Override
    public void visit(IfStatement node) {
        node.condition().accept(this);
        // Check if condition is bool type
        Type conditionType = ((Node)node.condition()).type();
        if (!(conditionType instanceof BoolType)) {
            node.setType(new ErrorType("IfStat requires bool condition not " + conditionType + "."));
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
        }

        node.thenBranch().accept(this);
        if (node.elseBranch() != null) {
            node.elseBranch().accept(this);
        }
    }

    @Override
    public void visit(WhileStatement node) {
        node.condition().accept(this);
        
        // Check if condition is bool type
        Type conditionType = ((Node)node.condition()).type();
        if (!(conditionType instanceof BoolType)) {
            node.setType(new ErrorType("WhileStat requires bool condition not " + conditionType + "."));
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
        }
        
        node.body().accept(this);
    }

    @Override
    public void visit(RepeatStatement node) {
        node.body().accept(this);
        node.condition().accept(this);
        
        // Check if condition is bool type
        Type conditionType = ((Node)node.condition()).type();
        if (!(conditionType instanceof BoolType)) {
            node.setType(new ErrorType("RepeatStat requires bool condition not " + conditionType + "."));
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType)node.type()).message());
        }
    }

    @Override
    public void visit(ReturnStatement node) {
        if (node.expression() != null) {
            node.expression().accept(this);
            
            // Check return type compatibility
            Type returnExprType = ((Node)node.expression()).type();
            Type expectedReturnType = getCurrentFunctionReturnType();
            
            if (expectedReturnType != null) {
                if (expectedReturnType instanceof VoidType) {
                    // Function should return void, but has a return expression
                    String errorMessage = "Function " + getCurrentFunctionName() + " returns " + returnExprType + " instead of void.";
                    node.setType(new ErrorType(errorMessage));
                    reportError(node.lineNumber(), node.charPosition(), errorMessage);
                } else if (!areTypesCompatible(expectedReturnType, returnExprType)) {
                    // Return type mismatch
                    String errorMessage = "Function " + getCurrentFunctionName() + " returns " + returnExprType + " instead of " + expectedReturnType + ".";
                    node.setType(new ErrorType(errorMessage));
                    reportError(node.lineNumber(), node.charPosition(), errorMessage);
                }
            }
        } else {
            // No return expression - check if function expects void
            Type expectedReturnType = getCurrentFunctionReturnType();
            if (expectedReturnType != null && !(expectedReturnType instanceof VoidType)) {
                String errorMessage = "Function " + getCurrentFunctionName() + " should return " + expectedReturnType + " but returns void.";
                node.setType(new ErrorType(errorMessage));
                reportError(node.lineNumber(), node.charPosition(), errorMessage);
            }
        }
    }

    @Override
    public void visit(StatementSequence node) {
        for (Statement stmt : node.statements()) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(VariableDeclaration node) {
        // Store variable types from declarations
        Type declarationType = node.type();
        
        for (Symbol symbol : node.variables()) {
            
            // Set the type on the symbol
            symbol.setType(declarationType);
            
            
            // Check array sizes for this variable
            checkArraySizes(symbol.name(), declarationType, node.lineNumber(), node.charPosition());
        }
    }
    
    private void checkArraySizes(String varName, Type type, int lineNum, int charPos) {
        if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            int size = arrayType.size();
            if (size <= 0) {
                reportError(lineNum, charPos, 
                    "Array " + varName + " has invalid size " + size + ".");
            }
            // Recursively check inner array dimensions
            checkArraySizes(varName, arrayType.baseType(), lineNum, charPos);
        }
    }
    
    private Type lookupFunctionSignature(FunctionCall node) {
        String functionName = node.function().name();
        
        // Collect argument types
        java.util.List<Type> argTypes = new java.util.ArrayList<>();
        for (Expression arg : node.arguments().arguments()) {
            argTypes.add(((Node)arg).type());
        }
        
        // Check for error types in arguments first
        for (Type argType : argTypes) {
            if (argType instanceof ErrorType) {
                String errorMessage = "Call with args (" + formatArgTypes(argTypes) + ") matches no function signature.";
                reportError(node.lineNumber(), node.charPosition(), errorMessage);
                return new ErrorType(errorMessage);
            }
        }
        
        // Look up function in registry
        if (!functionRegistry.containsKey(functionName)) {
            String errorMessage = "Call with args (" + formatArgTypes(argTypes) + ") matches no function signature.";
            reportError(node.lineNumber(), node.charPosition(), errorMessage);
            return new ErrorType(errorMessage);
        }
        
        // Find matching signature
        java.util.List<FunctionSignature> signatures = functionRegistry.get(functionName);
        for (FunctionSignature signature : signatures) {
            if (signature.matches(argTypes, this)) {
                return signature.returnType();
            }
        }
        
        // No matching signature found
        String errorMessage = "Call with args (" + formatArgTypes(argTypes) + ") matches no function signature.";
        reportError(node.lineNumber(), node.charPosition(), errorMessage);
        return new ErrorType(errorMessage);
    }
    
    private String formatArgTypes(java.util.List<Type> argTypes) {
        if (argTypes.isEmpty()) {
            return "empty";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < argTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(argTypes.get(i).toString());
        }
        return sb.toString();
    }
    
    private String getCurrentFunctionName() {
        return currentFunctionName;
    }
    
    private Type getCurrentFunctionReturnType() {
        return currentFunctionReturnType;
    }

    @Override
    public void visit(FunctionBody node) {
        for (VariableDeclaration varDecl : node.variables()) {
            varDecl.accept(this);
        }
        node.statements().accept(this);
    }

    @Override
    public void visit(FunctionDeclaration node) {
        // Extract function signature information
        String functionName = node.function().name();
        Type returnType = node.returnType();
        
        // Set current function context for return statement checking
        currentFunctionName = functionName;
        currentFunctionReturnType = returnType;
        
        // Extract parameter types from the function symbol
        java.util.List<Type> parameterTypes = new java.util.ArrayList<>();
        if (node.function().type() instanceof types.FuncType) {
            types.FuncType funcType = (types.FuncType) node.function().type();
            parameterTypes = funcType.params().getList();
        }
        
        // Create function signature
        FunctionSignature signature = new FunctionSignature(functionName, parameterTypes, returnType);
        
        // Store in function registry
        if (!functionRegistry.containsKey(functionName)) {
            functionRegistry.put(functionName, new java.util.ArrayList<>());
        }
        functionRegistry.get(functionName).add(signature);
        
        // Visit function body
        node.body().accept(this);
        
        // Clear current function context
        currentFunctionName = null;
        currentFunctionReturnType = null;
    }

    @Override
    public void visit(DeclarationList node) {
        for (VariableDeclaration decl : node.declarations()) {
            decl.accept(this);
        }
    }
}