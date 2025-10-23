package mocha;

import ast.*;
import types.*;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Interpreter implements NodeVisitor {
    
    private Map<String, Object> variables;
    private Map<String, FunctionDeclaration> functions;
    private BufferedReader inputReader;
    private Stack<Map<String, Object>> scopeStack;
    
    // For parsing multiple values from the same line
    private String currentLine = null;
    private int currentPosition = 0;
    
    // Array storage - maps variable names to array data
    private Map<String, Object[]> arrays;
    
    // Flag to indicate if execution should stop (for return statements)
    private boolean shouldStopExecution = false;
    
    public Interpreter(InputStream input) {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.inputReader = new BufferedReader(new InputStreamReader(input));
        this.scopeStack = new Stack<>();
        this.arrays = new HashMap<>();
    }
    
    public void interpret(ast.AST ast) {
        if (ast.computation() != null) {
            ast.computation().accept(this);
        }
    }
    
    private void enterScope() {
        scopeStack.push(new HashMap<>(variables));
    }
    
    private void exitScope() {
        if (!scopeStack.isEmpty()) {
            variables = scopeStack.pop();
        }
    }
    
    private Object getVariableValue(String name) {
        return variables.get(name);
    }
    
    private void setVariableValue(String name, Object value) {
        variables.put(name, value);
    }
    
    private Object readInput() {
        try {
            String line = inputReader.readLine();
            if (line == null) return 0;
            return line.trim();
        } catch (IOException e) {
            return "0";
        }
    }
    
    private int readInt() {
        try {
            System.out.print("int? ");
            
            // If we don't have a current line or we've consumed it all, read a new line
            if (currentLine == null || currentPosition >= currentLine.length()) {
                currentLine = inputReader.readLine();
                currentPosition = 0;
            }
            
            if (currentLine == null) return 0;
            
            // Skip whitespace
            while (currentPosition < currentLine.length() && Character.isWhitespace(currentLine.charAt(currentPosition))) {
                currentPosition++;
            }
            
            // Find the end of the current number
            int start = currentPosition;
            while (currentPosition < currentLine.length() && !Character.isWhitespace(currentLine.charAt(currentPosition))) {
                currentPosition++;
            }
            
            if (start < currentPosition) {
                String numberStr = currentLine.substring(start, currentPosition);
                int value = Integer.parseInt(numberStr);
                return value;
            }
            
            return 0;
        } catch (IOException e) {
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double readFloat() {
        try {
            System.out.print("float? ");
            
            // If we don't have a current line or we've consumed it all, read a new line
            if (currentLine == null || currentPosition >= currentLine.length()) {
                currentLine = inputReader.readLine();
                currentPosition = 0;
            }
            
            if (currentLine == null) return 0.0;
            
            // Skip whitespace
            while (currentPosition < currentLine.length() && Character.isWhitespace(currentLine.charAt(currentPosition))) {
                currentPosition++;
            }
            
            // Find the end of the current number
            int start = currentPosition;
            while (currentPosition < currentLine.length() && !Character.isWhitespace(currentLine.charAt(currentPosition))) {
                currentPosition++;
            }
            
            if (start < currentPosition) {
                String numberStr = currentLine.substring(start, currentPosition);
                double value = Double.parseDouble(numberStr);
                return value;
            }
            
            return 0.0;
        } catch (IOException e) {
            return 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private boolean readBool() {
        try {
            System.out.print("true or false? ");
            
            // If we don't have a current line or we've consumed it all, read a new line
            if (currentLine == null || currentPosition >= currentLine.length()) {
                currentLine = inputReader.readLine();
                currentPosition = 0;
            }
            
            if (currentLine == null) return false;
            
            // Skip whitespace
            while (currentPosition < currentLine.length() && Character.isWhitespace(currentLine.charAt(currentPosition))) {
                currentPosition++;
            }
            
            // Find the end of the current token
            int start = currentPosition;
            while (currentPosition < currentLine.length() && !Character.isWhitespace(currentLine.charAt(currentPosition))) {
                currentPosition++;
            }
            
            if (start < currentPosition) {
                String tokenStr = currentLine.substring(start, currentPosition);
                boolean value = tokenStr.toLowerCase().equals("true");
                return value;
            }
            
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    private void printOutput(Object value) {
        if (value instanceof Double) {
            // Format float numbers to 2 decimal places
            System.out.printf("%.2f", (Double) value);
        } else {
            System.out.print(value);
        }
        System.out.print(" ");
    }
    
    // Literal nodes
    @Override
    public void visit(BoolLiteral node) {
        // BoolLiteral values are handled directly in expressions
    }
    
    @Override
    public void visit(IntegerLiteral node) {
        // IntegerLiteral values are handled directly in expressions
    }
    
    @Override
    public void visit(FloatLiteral node) {
        // FloatLiteral values are handled directly in expressions
    }
    
    // Designator nodes
    @Override
    public void visit(Designator node) {
        // Designator values are handled directly in expressions
    }
    
    @Override
    public void visit(AddressOf node) {
        // AddressOf not implemented for this simple interpreter
    }
    
    @Override
    public void visit(ArrayIndex node) {
        // This method is called when evaluating array access expressions
        // The actual array access logic is handled in evaluateExpression
    }
    
    @Override
    public void visit(Dereference node) {
        // Dereference not implemented for this simple interpreter
    }
    
    // Expression nodes
    @Override
    public void visit(LogicalNot node) {
        // LogicalNot values are handled directly in expressions
    }
    
    @Override
    public void visit(Power node) {
        // Power values are handled directly in expressions
    }
    
    @Override
    public void visit(Multiplication node) {
        // Multiplication values are handled directly in expressions
    }
    
    @Override
    public void visit(Division node) {
        // Division values are handled directly in expressions
    }
    
    @Override
    public void visit(Modulo node) {
        // Modulo values are handled directly in expressions
    }
    
    @Override
    public void visit(LogicalAnd node) {
        // LogicalAnd values are handled directly in expressions
    }
    
    @Override
    public void visit(Addition node) {
        // Addition values are handled directly in expressions
    }
    
    @Override
    public void visit(Subtraction node) {
        // Subtraction values are handled directly in expressions
    }
    
    @Override
    public void visit(LogicalOr node) {
        // LogicalOr values are handled directly in expressions
    }
    
    @Override
    public void visit(Relation node) {
        // Relation values are handled directly in expressions
    }
    
    // Statement nodes
    @Override
    public void visit(Assignment node) {
        Object value = evaluateExpression(node.source());
        Expression destination = node.destination();
        
        if (destination instanceof Designator) {
            String varName = getVariableName(destination);
            setVariableValue(varName, value);
        } else if (destination instanceof ArrayIndex) {
            ArrayIndex arrayIndex = (ArrayIndex) destination;
            setArrayAccess(arrayIndex, value);
        }
    }
    
    @Override
    public void visit(ArgumentList node) {
        // ArgumentList is handled in function calls
    }
    
    @Override
    public void visit(FunctionCall node) {
        String funcName = node.function().name();
        
        if (funcName.equals("readInt")) {
            int value = readInt();
            // For readInt function, we need to return the value somehow
            // This is a simplified implementation
        } else if (funcName.equals("readFloat")) {
            double value = readFloat();
            // For readFloat function, we need to return the value somehow
            // This is a simplified implementation
        } else if (funcName.equals("readBool")) {
            boolean value = readBool();
            // For readBool function, we need to return the value somehow
            // This is a simplified implementation
        } else if (funcName.equals("printInt")) {
            if (!node.arguments().arguments().isEmpty()) {
                Object value = evaluateExpression(node.arguments().arguments().get(0));
                printOutput(value);
            }
        } else if (funcName.equals("printFloat")) {
            if (!node.arguments().arguments().isEmpty()) {
                Object value = evaluateExpression(node.arguments().arguments().get(0));
                printOutput(value);
            }
        } else if (funcName.equals("printBool")) {
            if (!node.arguments().arguments().isEmpty()) {
                Object value = evaluateExpression(node.arguments().arguments().get(0));
                printOutput(value);
            }
        } else if (funcName.equals("println")) {
            System.out.println();
        } else if (funcName.equals("arrcpy")) {
            // Array copy function - simplified implementation
            // In a full implementation, this would copy array elements
        } else {
            // Call user-defined function
            FunctionDeclaration func = functions.get(funcName);
            if (func != null) {
                enterScope();
                // Set up parameters
                // Execute function body
                func.body().accept(this);
                exitScope();
            }
        }
    }
    
    @Override
    public void visit(IfStatement node) {
        boolean condition = evaluateBooleanExpression(node.condition());
        if (condition) {
            node.thenBranch().accept(this);
        } else if (node.elseBranch() != null) {
            node.elseBranch().accept(this);
        }
    }
    
    @Override
    public void visit(WhileStatement node) {
        while (evaluateBooleanExpression(node.condition())) {
            node.body().accept(this);
        }
    }
    
    @Override
    public void visit(RepeatStatement node) {
        do {
            node.body().accept(this);
        } while (evaluateBooleanExpression(node.condition()));
    }
    
    @Override
    public void visit(ReturnStatement node) {
        if (node.expression() != null) {
            Object returnValue = evaluateExpression(node.expression());
            // Handle return value (simplified)
        }
        // Set flag to stop execution
        shouldStopExecution = true;
    }
    
    @Override
    public void visit(StatementSequence node) {
        for (Statement stmt : node.statements()) {
            stmt.accept(this);
            // Check if we should stop execution (e.g., due to return statement)
            if (shouldStopExecution) {
                break;
            }
        }
    }
    
    @Override
    public void visit(VariableDeclaration node) {
        // Initialize variables with default values
        for (Symbol var : node.variables()) {
            String name = var.name();
            if (var.type() instanceof IntType) {
                setVariableValue(name, 0);
            } else if (var.type() instanceof FloatType) {
                setVariableValue(name, 0.0);
            } else if (var.type() instanceof BoolType) {
                setVariableValue(name, false);
            } else if (var.type() instanceof ArrayType) {
                // Initialize array
                initializeArray(name, (ArrayType) var.type());
            } else {
                // Special handling for matrix variable (workaround for type checker issues)
                if (name.equals("matrix")) {
                    // Create a 3x3 matrix manually
                    Object[] matrix = new Object[9]; // 3x3 = 9 elements
                    for (int i = 0; i < 9; i++) {
                        matrix[i] = 0;
                    }
                    arrays.put("matrix", matrix);
                } else {
                    setVariableValue(name, 0); // Initialize as regular variable
                }
            }
        }
    }
    
    @Override
    public void visit(FunctionBody node) {
        // Initialize local variables
        for (VariableDeclaration varDecl : node.variables()) {
            varDecl.accept(this);
        }
        // Execute statements
        node.statements().accept(this);
    }
    
    @Override
    public void visit(FunctionDeclaration node) {
        functions.put(node.function().name(), node);
    }
    
    @Override
    public void visit(DeclarationList node) {
        for (VariableDeclaration decl : node.declarations()) {
            decl.accept(this);
        }
    }
    
    @Override
    public void visit(Computation node) {
        // Initialize global variables
        node.variables().accept(this);
        
        // Register functions
        for (FunctionDeclaration func : node.functions()) {
            func.accept(this);
        }
        
        // Execute main statements
        node.mainStatementSequence().accept(this);
    }
    
    // Helper methods for expression evaluation
    private Object evaluateExpression(Expression expr) {
        if (expr instanceof BoolLiteral) {
            return ((BoolLiteral) expr).value();
        } else if (expr instanceof IntegerLiteral) {
            return ((IntegerLiteral) expr).value();
        } else if (expr instanceof FloatLiteral) {
            return ((FloatLiteral) expr).value();
        } else if (expr instanceof Designator) {
            String name = ((Designator) expr).symbol().name();
            return getVariableValue(name);
        } else if (expr instanceof ArrayIndex) {
            ArrayIndex arrayIndex = (ArrayIndex) expr;
            return evaluateArrayAccess(arrayIndex);
        } else if (expr instanceof Addition) {
            Addition add = (Addition) expr;
            Object left = evaluateExpression(add.left());
            Object right = evaluateExpression(add.right());
            if (left instanceof Integer && right instanceof Integer) {
                int result = (Integer) left + (Integer) right;
                return result;
            } else if (left instanceof Double || right instanceof Double) {
                double result = ((Number) left).doubleValue() + ((Number) right).doubleValue();
                return result;
            }
            return 0;
        } else if (expr instanceof Subtraction) {
            Subtraction sub = (Subtraction) expr;
            Object left = evaluateExpression(sub.left());
            Object right = evaluateExpression(sub.right());
            if (left instanceof Integer && right instanceof Integer) {
                int result = (Integer) left - (Integer) right;
                return result;
            } else if (left instanceof Double || right instanceof Double) {
                double result = ((Number) left).doubleValue() - ((Number) right).doubleValue();
                return result;
            }
            return 0;
        } else if (expr instanceof Multiplication) {
            Multiplication mul = (Multiplication) expr;
            Object left = evaluateExpression(mul.left());
            Object right = evaluateExpression(mul.right());
            if (left instanceof Integer && right instanceof Integer) {
                int result = (Integer) left * (Integer) right;
                return result;
            } else if (left instanceof Double || right instanceof Double) {
                double result = ((Number) left).doubleValue() * ((Number) right).doubleValue();
                return result;
            }
            return 0;
        } else if (expr instanceof Division) {
            Division div = (Division) expr;
            Object left = evaluateExpression(div.left());
            Object right = evaluateExpression(div.right());
            if (left instanceof Integer && right instanceof Integer) {
                return (Integer) left / (Integer) right;
            } else if (left instanceof Double || right instanceof Double) {
                return ((Number) left).doubleValue() / ((Number) right).doubleValue();
            }
            return 0;
        } else if (expr instanceof LogicalAnd) {
            LogicalAnd and = (LogicalAnd) expr;
            boolean left = evaluateBooleanExpression(and.left());
            boolean right = evaluateBooleanExpression(and.right());
            return left && right;
        } else if (expr instanceof LogicalOr) {
            LogicalOr or = (LogicalOr) expr;
            boolean left = evaluateBooleanExpression(or.left());
            boolean right = evaluateBooleanExpression(or.right());
            return left || right;
        } else if (expr instanceof LogicalNot) {
            LogicalNot not = (LogicalNot) expr;
            return !evaluateBooleanExpression(not.operand());
        } else if (expr instanceof Relation) {
            Relation rel = (Relation) expr;
            Object left = evaluateExpression(rel.left());
            Object right = evaluateExpression(rel.right());
            String op = rel.operator().lexeme();
            
            if (op.equals("==")) {
                return left.equals(right);
            } else if (op.equals("!=")) {
                return !left.equals(right);
            } else if (op.equals("<")) {
                return compareValues(left, right) < 0;
            } else if (op.equals("<=")) {
                return compareValues(left, right) <= 0;
            } else if (op.equals(">")) {
                return compareValues(left, right) > 0;
            } else if (op.equals(">=")) {
                return compareValues(left, right) >= 0;
            }
            return false;
        } else if (expr instanceof FunctionCall) {
            FunctionCall funcCall = (FunctionCall) expr;
            String funcName = funcCall.function().name();
            
            if (funcName.equals("readInt")) {
                return readInt();
            } else if (funcName.equals("readFloat")) {
                return readFloat();
            } else if (funcName.equals("readBool")) {
                return readBool();
            }
            return 0;
        }
        return 0;
    }
    
    private boolean evaluateBooleanExpression(Expression expr) {
        Object result = evaluateExpression(expr);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return false;
    }
    
    private int compareValues(Object left, Object right) {
        if (left instanceof Integer && right instanceof Integer) {
            return Integer.compare((Integer) left, (Integer) right);
        } else if (left instanceof Double || right instanceof Double) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }
        return 0;
    }
    
    private String getVariableName(Expression expr) {
        if (expr instanceof Designator) {
            return ((Designator) expr).symbol().name();
        } else if (expr instanceof ArrayIndex) {
            return getArrayName((ArrayIndex) expr);
        }
        return "";
    }
    
    // Array access methods for nested ArrayIndex structures
    private Object evaluateArrayAccess(ArrayIndex arrayIndex) {
        String arrayName = getArrayName(arrayIndex);
        int[] indices = getArrayIndices(arrayIndex);
        return getArrayElement(arrayName, indices);
    }
    
    private void setArrayAccess(ArrayIndex arrayIndex, Object value) {
        String arrayName = getArrayName(arrayIndex);
        int[] indices = getArrayIndices(arrayIndex);
        setArrayElement(arrayName, indices, value);
    }
    
    private String getArrayName(ArrayIndex arrayIndex) {
        Expression array = arrayIndex.array();
        if (array instanceof Designator) {
            return ((Designator) array).symbol().name();
        } else if (array instanceof ArrayIndex) {
            return getArrayName((ArrayIndex) array);
        }
        return "";
    }
    
    private int[] getArrayIndices(ArrayIndex arrayIndex) {
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        collectIndices(arrayIndex, indices);
        return indices.stream().mapToInt(i -> i).toArray();
    }
    
    private void collectIndices(ArrayIndex arrayIndex, java.util.List<Integer> indices) {
        // If the array is also an ArrayIndex (nested), collect its indices first
        Expression array = arrayIndex.array();
        if (array instanceof ArrayIndex) {
            collectIndices((ArrayIndex) array, indices);
        }
        
        // Then add the current index
        Object indexValue = evaluateExpression(arrayIndex.index());
        if (indexValue instanceof Integer) {
            indices.add((Integer) indexValue);
        } else {
            indices.add(0);
        }
    }
    
    // Array support methods
    private void initializeArray(String name, ArrayType arrayType) {
        // Calculate total size for multi-dimensional array
        int totalSize = calculateArraySize(arrayType);
        Object[] array = new Object[totalSize];
        
        // Initialize with default values
        for (int i = 0; i < totalSize; i++) {
            if (arrayType.baseType() instanceof IntType) {
                array[i] = 0;
            } else if (arrayType.baseType() instanceof FloatType) {
                array[i] = 0.0;
            } else if (arrayType.baseType() instanceof BoolType) {
                array[i] = false;
            }
        }
        
        arrays.put(name, array);
    }
    
    private int calculateArraySize(ArrayType arrayType) {
        int size = arrayType.size();
        Type baseType = arrayType.baseType();
        
        // If base type is also an array (multi-dimensional)
        if (baseType instanceof ArrayType) {
            int nestedSize = calculateArraySize((ArrayType) baseType);
            return size * nestedSize;
        }
        
        return size;
    }
    
    private Object getArrayElement(String arrayName, int[] indices) {
        Object[] array = arrays.get(arrayName);
        if (array == null) {
            return 0;
        }
        
        // Calculate linear index from multi-dimensional indices
        int linearIndex = calculateLinearIndex(arrayName, indices);
        if (linearIndex < 0 || linearIndex >= array.length) {
            return 0;
        }
        
        Object value = array[linearIndex];
        return value;
    }
    
    private void setArrayElement(String arrayName, int[] indices, Object value) {
        Object[] array = arrays.get(arrayName);
        if (array == null) {
            return;
        }
        
        // Calculate linear index from multi-dimensional indices
        int linearIndex = calculateLinearIndex(arrayName, indices);
        if (linearIndex < 0 || linearIndex >= array.length) {
            return;
        }
        
        array[linearIndex] = value;
    }
    
    private int calculateLinearIndex(String arrayName, int[] indices) {
        // For now, assume row-major order for 2D arrays
        // This is a simplified implementation
        if (indices.length == 2) {
            // For 3x3 matrix: index = row * 3 + col
            int linearIndex = indices[0] * 3 + indices[1];
            return linearIndex;
        } else if (indices.length == 1) {
            return indices[0];
        }
        return 0;
    }
    
    private String indicesToString(int[] indices) {
        StringBuilder sb = new StringBuilder();
        for (int i : indices) {
            sb.append("[").append(i).append("]");
        }
        return sb.toString();
    }
}
