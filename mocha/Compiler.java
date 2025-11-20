package mocha;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.io.InputStream;

import ast.AST;
import ast.Computation;

// mocha imports
import mocha.Scanner;
import mocha.Symbol;
import mocha.Token;
import mocha.NonTerminal;

// types imports
import types.ErrorType;
import types.VoidType;

import ir.IRProgram;
import ir.build.IRGenerator;
import ir.optimize.Optimizer;

public class Compiler {

    // Error Reporting ============================================================
    private StringBuilder errorBuffer = new StringBuilder();
    
    // Store the parsed AST for interpretation
    private ast.AST parsedAST = null;

    private String reportSyntaxError (NonTerminal nt) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected a token from " + nt.name() + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportSyntaxError (Token.Kind kind) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected " + kind + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    public String errorReport () {
        return errorBuffer.toString();
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }

    private class QuitParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public QuitParseException (String errorMessage) {
            super(errorMessage);
        }
    }
    
    private class FunctionSignature {
        private String name;
        private types.TypeList params;
        private types.Type returnType;
        private int lineNumber;
        
        public FunctionSignature(String name, types.TypeList params, types.Type returnType, int lineNumber) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.lineNumber = lineNumber;
        }
        
        public String name() { return name; }
        public types.TypeList params() { return params; }
        public types.Type returnType() { return returnType; }
        public int lineNumber() { return lineNumber; }
    }

    private void addBuiltInFunctions() {
        // Add printInt(int): void
        types.TypeList intParams = new types.TypeList();
        intParams.append(new types.IntType());
        functionSignatures.add(new FunctionSignature("printInt", intParams, new types.VoidType(), 0));
        
        types.TypeList floatParams = new types.TypeList();
        floatParams.append(new types.FloatType());
        functionSignatures.add(new FunctionSignature("printFloat", floatParams, new types.VoidType(), 0));
        
        types.TypeList boolParams = new types.TypeList();
        boolParams.append(new types.BoolType());
        functionSignatures.add(new FunctionSignature("printBool", boolParams, new types.VoidType(), 0));
    }

    private int lineNumber () {
        return currentToken.lineNumber();
    }

    private int charPosition () {
        return currentToken.charPosition();
    }

    // Compiler ===================================================================
    private Scanner scanner;
    private Token currentToken;

    private int numDataRegisters; // available registers are [1..numDataRegisters]
    private List<Integer> instructions;

    // Need to map from IDENT to memory offset

    public Compiler (Scanner scanner, int numRegs) {
        this.scanner = scanner;
        currentToken = this.scanner.next();
        numDataRegisters = numRegs;
        instructions = new ArrayList<>();
    }

    public ast.AST genAST() {
        initSymbolTable();
        try {
            ast.Computation computation = computation();
            parsedAST = new ast.AST(computation);
            return parsedAST;
        } catch (QuitParseException q) {
            parsedAST = new ast.AST(null);
            return parsedAST;
        }
    }
    
    public void interpret(InputStream in) {
        // Use the already parsed AST instead of re-parsing
        if (parsedAST == null) {
            System.out.println("Error: No AST available for interpretation");
            return;
        }
        
        // Create and run interpreter
        Interpreter interpreter = new Interpreter(in);
        interpreter.interpret(parsedAST);
    }

    public int[] compile () {
        initSymbolTable();
        try {
            computation();
            return instructions.stream().mapToInt(Integer::intValue).toArray();
        }
        catch (QuitParseException q) {
            // errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            // errorBuffer.append("[Could not complete parsing.]");
            return new ArrayList<Integer>().stream().mapToInt(Integer::intValue).toArray();
        }
    }

    // SymbolTable Management =====================================================
    private SymbolTable symbolTable;
    private java.util.Stack<SymbolTable> scopeStack;
    private java.util.List<FunctionSignature> functionSignatures;

    private void initSymbolTable () {
        symbolTable = new SymbolTable();
        scopeStack = new java.util.Stack<>();
        scopeStack.push(symbolTable); // Global scope
        functionSignatures = new java.util.ArrayList<>();
        
        // Add built-in functions to the functionSignatures list
        addBuiltInFunctions();
    }

    private void enterScope () {
        // Create a new local scope
        SymbolTable newScope = new SymbolTable();
        scopeStack.push(newScope);
        symbolTable = newScope;
    }

    private void exitScope () {
        // Return to previous scope
        if (scopeStack.size() > 1) {
            scopeStack.pop();
            symbolTable = scopeStack.peek();
        }
    }

    private Symbol tryResolveVariable (Token ident) {
        // Search through all scopes from innermost to outermost
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            try {
                return scopeStack.get(i).lookup(ident.lexeme());
            } catch (SymbolNotFoundError e) {
                // Continue searching in outer scopes
            }
        }
        // Symbol not found in any scope
        reportResolveSymbolError(ident.lexeme(), lineNumber(), charPosition());
        return new Symbol(ident.lexeme(), new ErrorType("Unknown symbol"), false);
    }

    private Symbol tryDeclareVariable (Token ident, types.Type type) {
        // System.out.println("Trying to declare variable: " + ident.lexeme() + " with type: " + type);
        try {
            return symbolTable.insert(ident.lexeme(), type, false);
        } catch (RedeclarationError e) {
            // Report redeclaration error
            reportDeclareSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
            return new Symbol(ident.lexeme(), type, false);
        }
    }

    private String reportResolveSymbolError (String name, int lineNum, int charPos) {
        String message = "SymbolError(" + lineNum + ")";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportDeclareSymbolError (String name, int lineNum, int charPos) {
        String message = "SymbolError(" + lineNum + ")";
        errorBuffer.append(message + "\n");
        return message;
    }
    
    private String reportTypeError (int lineNum, int charPos, String message) {
        String errorMessage = "TypeError(" + lineNum + "," + charPos + ")[" + message + "]";
        errorBuffer.append(errorMessage + "\n");
        return errorMessage;
    }


    // Helper Methods =============================================================
    private boolean have (Token.Kind kind) {
        return currentToken.kind == kind;
    }

    private boolean have (NonTerminal nt) {
        return nt.firstSet().contains(currentToken.kind);
    }

    private boolean accept (Token.Kind kind) {
        if (have(kind)) {
            try {
                currentToken = scanner.next();
            }
            catch (NoSuchElementException e) {
                if (!kind.equals(Token.Kind.EOF)) {
                    String errorMessage = reportSyntaxError(kind);
                    throw new QuitParseException(errorMessage);
                }
            }
            return true;
        }
        return false;
    }

    private boolean accept (NonTerminal nt) {
        if (have(nt)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }

    private boolean expect (Token.Kind kind) {
        if (accept(kind)) {
            return true;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private boolean expect (NonTerminal nt) {
        if (accept(nt)) {
            return true;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (Token.Kind kind) {
        Token tok = currentToken;
        if (accept(kind)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (NonTerminal nt) {
        Token tok = currentToken;
        if (accept(nt)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }


// Grammar Rules ==============================================================

    // function for matching rule that only expects nonterminal's FIRST set
    private Token matchNonTerminal (NonTerminal nt) {
        return expectRetrieve(nt);
    }

    // TODO: copy operators and type grammar rules from Compiler

    // Grammar rule implementations
    
    // varDecl = typeDecl ident { "," ident } ";"
    private ast.VariableDeclaration varDecl() {
        int declLineNum = lineNumber();
        int declCharPos = charPosition();
        
        types.Type type = typeDecl();
        java.util.List<Symbol> variables = new java.util.ArrayList<>();
        
        Token ident = expectRetrieve(Token.Kind.IDENT);
        variables.add(tryDeclareVariable(ident, type));
        
        while (accept(Token.Kind.COMMA)) {
            ident = expectRetrieve(Token.Kind.IDENT);
            variables.add(tryDeclareVariable(ident, type));
        }
        
        expect(Token.Kind.SEMICOLON);
        return new ast.VariableDeclaration(declLineNum, declCharPos, type, variables);
    }
    
    
    
    // typeDecl = type { "[" integerLit "]" }
    private types.Type typeDecl() {
        types.Type baseType = type();
        
        // Handle multiple array dimensions
        while (accept(Token.Kind.OPEN_BRACKET)) {
            Token sizeToken = expectRetrieve(Token.Kind.INT_VAL);
            int size = Integer.parseInt(sizeToken.lexeme());
            expect(Token.Kind.CLOSE_BRACKET);
            
            baseType = new types.ArrayType(baseType, size);
        }
        
        return baseType;
    }
    
    // type = "bool" | "int" | "float"
    private types.Type type() {
        if (accept(Token.Kind.BOOL)) {
            return new types.BoolType();
        } else if (accept(Token.Kind.INT)) {
            return new types.IntType();
        } else if (accept(Token.Kind.FLOAT)) {
            return new types.FloatType();
        } else {
            String errorMessage = reportSyntaxError(Token.Kind.BOOL);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // funcDecl = "function" ident formalParam ":" ( "void" | type ) funcBody
    private ast.FunctionDeclaration funcDecl() {
        expect(Token.Kind.FUNC);
        Token ident = expectRetrieve(Token.Kind.IDENT);
        
        // Create function symbol and insert into symbol table
        Symbol funcSymbol = new Symbol(ident.lexeme(), new types.FuncType(new types.TypeList(), new types.VoidType()), true);
        
        formalParam(funcSymbol);
        expect(Token.Kind.COLON);
        
        types.Type returnType;
        if (accept(Token.Kind.VOID)) {
            returnType = new types.VoidType();
        } else {
            returnType = type();
        }
        
        // Update the function symbol with the correct return type
        types.FuncType currentFuncType = (types.FuncType) funcSymbol.type();
        funcSymbol.setType(new types.FuncType(currentFuncType.params(), returnType));
        
        // Check for duplicate function signature before inserting
        boolean hasDuplicateSignature = false;
        
        // Check against all existing function signatures
        for (FunctionSignature existingSig : functionSignatures) {
            if (existingSig.name().equals(ident.lexeme())) {
                // Compare parameter types manually since TypeList doesn't have equals()
                boolean sameParams = false;
                if (currentFuncType.params().size() == existingSig.params().size()) {
                    sameParams = true;
                    for (int i = 0; i < currentFuncType.params().size(); i++) {
                        types.Type currentParam = currentFuncType.params().get(i);
                        types.Type existingParam = existingSig.params().get(i);
                        if (!currentParam.getClass().equals(existingParam.getClass())) {
                            sameParams = false;
                            break;
                        }
                    }
                }
                
                if (sameParams) {
                    if (currentFuncType.returnType().equals(existingSig.returnType())) {
                        // Exact duplicate signature
                        hasDuplicateSignature = true;
                        reportDeclareSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
                    } else {
                        // Same parameters but different return type - also an error
                        hasDuplicateSignature = true;
                        reportDeclareSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
                    }
                }
            }
        }
        
        // Insert the function into the symbol table only if no duplicate signature
        if (!hasDuplicateSignature) {
            try {
                // Insert into global scope (first scope in the stack)
                SymbolTable globalScopeForInsert = scopeStack.get(0);
                globalScopeForInsert.insert(ident.lexeme(), funcSymbol.type(), true);
            } catch (RedeclarationError e) {
                // For function overloading, we need to allow multiple functions with the same name
                // but different signatures. Since the symbol table doesn't support this,
                // we'll skip insertion and let the type checker handle the final validation.
                // The function signature comparison above should have caught any conflicts.
            }
            
            // Add the function signature to our list for future comparisons
            functionSignatures.add(new FunctionSignature(ident.lexeme(), currentFuncType.params(), returnType, ident.lineNumber()));
        }
        
        ast.FunctionBody body = funcBody();
        return new ast.FunctionDeclaration(lineNumber(), charPosition(), funcSymbol, returnType, body);
    }
    
    // formalParam = "(" [ paramDecl { "," paramDecl } ] ")"
    private void formalParam(Symbol funcSymbol) {
        expect(Token.Kind.OPEN_PAREN);
        
        // Enter function scope for parameters
        enterScope();
        
        types.TypeList paramTypes = new types.TypeList();
        
        if (have(NonTerminal.PARAM_DECL)) {
            paramDecl(paramTypes);
            while (accept(Token.Kind.COMMA)) {
                paramDecl(paramTypes);
            }
        }
        
        expect(Token.Kind.CLOSE_PAREN);
        // Update the function symbol with the parameter types
        types.FuncType currentFuncType = (types.FuncType) funcSymbol.type();
        funcSymbol.setType(new types.FuncType(paramTypes, currentFuncType.returnType()));
    }
    
    // paramDecl = paramType ident
    private void paramDecl(types.TypeList paramTypes) {
        types.Type paramType = paramType();
        Token ident = expectRetrieve(Token.Kind.IDENT);
        paramTypes.append(paramType);
        // Insert parameter into current scope (function scope)
        try {
            symbolTable.insert(ident.lexeme(), paramType, false);
        } catch (RedeclarationError e) {
            // Parameter redeclaration within same function - this should be an error
            reportDeclareSymbolError(ident.lexeme(), lineNumber(), charPosition());
        }
    }
    
    // paramType = type { "[" "]" }
    private types.Type paramType() {
        types.Type baseType = type();
        
        // Collect all array dimensions first
        java.util.List<Integer> dimensions = new java.util.ArrayList<>();
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.CLOSE_BRACKET);
            dimensions.add(-1); // -1 indicates array parameter (no size specified)
        }
        
        // Build array type from outside in (reverse order)
        for (int i = dimensions.size() - 1; i >= 0; i--) {
            baseType = new types.ArrayType(baseType, dimensions.get(i));
        }
        
        return baseType;
    }
    
    // funcBody = "{" { varDecl } statSeq "}" ";"
    private ast.FunctionBody funcBody() {
        expect(Token.Kind.OPEN_BRACE);
        
        ast.FunctionBody body = new ast.FunctionBody(lineNumber(), charPosition());
        
        while (have(NonTerminal.VAR_DECL)) {
            body.addVariable(varDecl());
        }
        
        ast.StatementSequence statSeq = statSeq();
        for (ast.Statement stmt : statSeq.statements()) {
            body.statements().addStatement(stmt);
        }
        
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.SEMICOLON);
        
        // Exit function scope (parameters and local variables)
        exitScope();
        
        return body;
    }
    
    // statSeq = statement ";" { statement ";" }
    private ast.StatementSequence statSeq() {
        ast.StatementSequence seq = new ast.StatementSequence(lineNumber(), charPosition());
        
        seq.addStatement(statement());
        expect(Token.Kind.SEMICOLON);
        
        while (have(NonTerminal.STATEMENT)) {
            seq.addStatement(statement());
            expect(Token.Kind.SEMICOLON);
        }
        
        return seq;
    }
    
    // statement = assign | funcCall | ifStat | whileStat | repeatStat | returnStat
    private ast.Statement statement() {
        if (have(NonTerminal.ASSIGN)) {
            return assign();
        } else if (have(NonTerminal.FUNC_CALL)) {
            return funcCall();
        } else if (have(NonTerminal.IF_STAT)) {
            return ifStat();
        } else if (have(NonTerminal.WHILE_STAT)) {
            return whileStat();
        } else if (have(NonTerminal.REPEAT_STAT)) {
            return repeatStat();
        } else if (have(NonTerminal.RETURN_STAT)) {
            return returnStat();
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.STATEMENT);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // assign = designator ( ( assignOp relExpr ) | unaryOp )
    private ast.Assignment assign() {
        ast.Expression dest = designator();
        
        if (assignOp()) {
            ast.Expression src = relExpr();
            return new ast.Assignment(lineNumber(), charPosition(), dest, null, src);
        } else if (have(Token.Kind.UNI_INC) || have(Token.Kind.UNI_DEC)) {
            // Handle increment/decrement operations: a++ or a--
            Token op = currentToken;
            if (op.kind == Token.Kind.UNI_INC) {
                // a++ becomes a = a + 1
                accept(Token.Kind.UNI_INC); // Consume the token
                Token oneToken = new Token("1", lineNumber(), charPosition());
                ast.Expression src = new ast.Addition(lineNumber(), charPosition(), dest, op, new ast.IntegerLiteral(lineNumber(), charPosition(), oneToken));
                return new ast.Assignment(lineNumber(), charPosition(), dest, null, src);
            } else if (op.kind == Token.Kind.UNI_DEC) {
                // a-- becomes a = a - 1
                accept(Token.Kind.UNI_DEC); // Consume the token
                Token oneToken = new Token("1", lineNumber(), charPosition());
                ast.Expression src = new ast.Subtraction(lineNumber(), charPosition(), dest, op, new ast.IntegerLiteral(lineNumber(), charPosition(), oneToken));
                return new ast.Assignment(lineNumber(), charPosition(), dest, null, src);
            } else {
                return new ast.Assignment(lineNumber(), charPosition(), dest, null, null);
            }
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.ASSIGN);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // funcCall = "call" ident "(" [ relExpr { "," relExpr } ] ")"
    private ast.FunctionCall funcCall() {
        int callLine = lineNumber();
        int callPos = charPosition();
        expect(Token.Kind.CALL);
        Token ident = expectRetrieve(Token.Kind.IDENT);
        Symbol funcSymbol = tryResolveVariable(ident);
        
        expect(Token.Kind.OPEN_PAREN);
        ast.ArgumentList args = new ast.ArgumentList(lineNumber(), charPosition());
        
        if (have(NonTerminal.REL_EXPR)) {
            args.addArgument(relExpr());
            while (accept(Token.Kind.COMMA)) {
                args.addArgument(relExpr());
            }
        }
        
        expect(Token.Kind.CLOSE_PAREN);
        return new ast.FunctionCall(callLine, callPos, funcSymbol, args);
    }
    
    // ifStat = "if" relation "then" statSeq [ "else" statSeq ] "fi"
    private ast.IfStatement ifStat() {
        expect(Token.Kind.IF);
        int ifLine = lineNumber();
        int ifPos = charPosition();
        ast.Expression condition = relation();
        expect(Token.Kind.THEN);
        ast.StatementSequence thenBranch = statSeq();
        
        ast.StatementSequence elseBranch = null;
        if (accept(Token.Kind.ELSE)) {
            elseBranch = statSeq();
        }
        
        expect(Token.Kind.FI);
        return new ast.IfStatement(ifLine, ifPos, condition, thenBranch, elseBranch);
    }
    
    // whileStat = "while" relation "do" statSeq "od"
    private ast.WhileStatement whileStat() {
        int whileLine = lineNumber();
        int whilePos = charPosition();
        expect(Token.Kind.WHILE);
        ast.Expression condition = relation();
        expect(Token.Kind.DO);
        ast.StatementSequence body = statSeq();
        expect(Token.Kind.OD);
        return new ast.WhileStatement(whileLine, whilePos, condition, body);
    }
    
    // repeatStat = "repeat" statSeq "until" relation
    private ast.RepeatStatement repeatStat() {
        int repeatLine = lineNumber();
        int repeatPos = charPosition();
        expect(Token.Kind.REPEAT);
        ast.StatementSequence body = statSeq();
        expect(Token.Kind.UNTIL);
        ast.Expression condition = relation();
        return new ast.RepeatStatement(repeatLine, repeatPos, body, condition);
    }
    
    // returnStat = "return" [ relExpr ]
    private ast.ReturnStatement returnStat() {
        expect(Token.Kind.RETURN);
        ast.Expression expr = null;
        if (have(NonTerminal.REL_EXPR)) {
            expr = relExpr();
        }
        return new ast.ReturnStatement(lineNumber(), charPosition(), expr);
    }
    
    // relation = "(" relExpr ")"
    private ast.Expression relation() {
        expect(Token.Kind.OPEN_PAREN);
        ast.Expression expr = relExpr();
        expect(Token.Kind.CLOSE_PAREN);
        return expr;
    }
    
    // relExpr = addExpr { relOp addExpr }
    private ast.Expression relExpr() {
        ast.Expression left = addExpr();
        
        Token op = relOp();
        while (op != null) {
            ast.Expression right = addExpr();
            left = new ast.Relation(lineNumber(), charPosition(), left, op, right);
            op = relOp();
        }
        
        return left;
    }
    
    // addExpr = multExpr { addOp multExpr }
    private ast.Expression addExpr() {
        ast.Expression left = multExpr();
        
        Token op = addOp();
        while (op != null) {
            ast.Expression right = multExpr();
            if (op.kind == Token.Kind.ADD) {
                left = new ast.Addition(lineNumber(), charPosition(), left, op, right);
            } else if (op.kind == Token.Kind.SUB) {
                left = new ast.Subtraction(lineNumber(), charPosition(), left, op, right);
            } else if (op.kind == Token.Kind.OR) {
                left = new ast.LogicalOr(lineNumber(), charPosition(), left, op, right);
            }
            op = addOp();
        }
        
        return left;
    }
    
    // multExpr = powExpr { multOp powExpr }
    private ast.Expression multExpr() {
        ast.Expression left = powExpr();
        
        Token op = mulOp();
        while (op != null) {
            ast.Expression right = powExpr();
            if (op.kind == Token.Kind.MUL) {
                left = new ast.Multiplication(lineNumber(), charPosition(), left, op, right);
            } else if (op.kind == Token.Kind.DIV) {
                left = new ast.Division(lineNumber(), charPosition(), left, op, right);
            } else if (op.kind == Token.Kind.MOD) {
                left = new ast.Modulo(lineNumber(), charPosition(), left, op, right);
            } else if (op.kind == Token.Kind.AND) {
                left = new ast.LogicalAnd(lineNumber(), charPosition(), left, op, right);
            }
            op = mulOp();
        }
        
        return left;
    }
    
    // powExpr = groupExpr { powOp groupExpr }
    private ast.Expression powExpr() {
        ast.Expression left = groupExpr();
        
        Token op = powOp();
        while (op != null) {
            ast.Expression right = groupExpr();
            left = new ast.Power(lineNumber(), charPosition(), left, op, right);
            op = powOp();
        }
        
        return left;
    }
    
    // groupExpr = literal | designator | "not" relExpr | relation | funcCall
    private ast.Expression groupExpr() {
        if (have(NonTerminal.LITERAL)) {
            return literal();
        } else if (have(NonTerminal.DESIGNATOR)) {
            return designator();
        } else if (accept(Token.Kind.NOT)) {
            Token op = currentToken;
            ast.Expression operand = relExpr();
            return new ast.LogicalNot(lineNumber(), charPosition(), op, operand);
        } else if (have(NonTerminal.RELATION)) {
            return relation();
        } else if (have(NonTerminal.FUNC_CALL)) {
            return funcCall();
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.GROUP_EXPR);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // designator = ident { "[" relExpr "]" }
    private ast.Expression designator() {
        Token ident = expectRetrieve(Token.Kind.IDENT);
        Symbol symbol = tryResolveVariable(ident);
        
        ast.Expression result = new ast.Designator(lineNumber(), charPosition(), symbol);
        
        while (accept(Token.Kind.OPEN_BRACKET)) {
            ast.Expression index = relExpr();
            expect(Token.Kind.CLOSE_BRACKET);
            result = new ast.ArrayIndex(lineNumber(), charPosition(), result, index);
        }
        
        return result;
    }
    
    // literal = integerLit | floatLit | boolLit
    private ast.Expression literal() {
        Token tok = matchNonTerminal(NonTerminal.LITERAL);
        
        if (tok.kind == Token.Kind.INT_VAL) {
            return new ast.IntegerLiteral(lineNumber(), charPosition(), tok);
        } else if (tok.kind == Token.Kind.FLOAT_VAL) {
            return new ast.FloatLiteral(lineNumber(), charPosition(), tok);
        } else if (tok.kind == Token.Kind.TRUE || tok.kind == Token.Kind.FALSE) {
            return new ast.BoolLiteral(lineNumber(), charPosition(), tok);
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.LITERAL);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // Helper methods for operators
    private Token relOp() {
        if (have(Token.Kind.EQUAL_TO)) {
            Token op = currentToken;
            accept(Token.Kind.EQUAL_TO);
            return op;
        } else if (have(Token.Kind.NOT_EQUAL)) {
            Token op = currentToken;
            accept(Token.Kind.NOT_EQUAL);
            return op;
        } else if (have(Token.Kind.LESS_THAN)) {
            Token op = currentToken;
            accept(Token.Kind.LESS_THAN);
            return op;
        } else if (have(Token.Kind.LESS_EQUAL)) {
            Token op = currentToken;
            accept(Token.Kind.LESS_EQUAL);
            return op;
        } else if (have(Token.Kind.GREATER_THAN)) {
            Token op = currentToken;
            accept(Token.Kind.GREATER_THAN);
            return op;
        } else if (have(Token.Kind.GREATER_EQUAL)) {
            Token op = currentToken;
            accept(Token.Kind.GREATER_EQUAL);
            return op;
        }
        return null;
    }
    
    private Token addOp() {
        if (have(Token.Kind.ADD)) {
            Token op = currentToken;
            accept(Token.Kind.ADD);
            return op;
        } else if (have(Token.Kind.SUB)) {
            Token op = currentToken;
            accept(Token.Kind.SUB);
            return op;
        } else if (have(Token.Kind.OR)) {
            Token op = currentToken;
            accept(Token.Kind.OR);
            return op;
        }
        return null;
    }
    
    private Token mulOp() {
        if (have(Token.Kind.MUL)) {
            Token op = currentToken;
            accept(Token.Kind.MUL);
            return op;
        } else if (have(Token.Kind.DIV)) {
            Token op = currentToken;
            accept(Token.Kind.DIV);
            return op;
        } else if (have(Token.Kind.MOD)) {
            Token op = currentToken;
            accept(Token.Kind.MOD);
            return op;
        } else if (have(Token.Kind.AND)) {
            Token op = currentToken;
            accept(Token.Kind.AND);
            return op;
        }
        return null;
    }
    
    private Token powOp() {
        if (have(Token.Kind.POW)) {
            Token op = currentToken;
            accept(Token.Kind.POW);
            return op;
        }
        return null;
    }
    
    private boolean assignOp() {
        return accept(Token.Kind.ASSIGN) || accept(Token.Kind.ADD_ASSIGN) ||
               accept(Token.Kind.SUB_ASSIGN) || accept(Token.Kind.MUL_ASSIGN) ||
               accept(Token.Kind.DIV_ASSIGN) || accept(Token.Kind.MOD_ASSIGN) ||
               accept(Token.Kind.POW_ASSIGN);
    }
    
    private boolean unaryOp() {
        return accept(Token.Kind.UNI_INC) || accept(Token.Kind.UNI_DEC);
    }

    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private Computation computation () {
        expect(Token.Kind.MAIN);
        
        // Create main symbol
        Symbol mainSymbol = new Symbol("main", new VoidType(), false);
        
        // Parse variable declarations
        ast.DeclarationList varDecls = new ast.DeclarationList(lineNumber(), charPosition());
        while (have(NonTerminal.VAR_DECL)) {
            varDecls.addDeclaration(varDecl());
        }
        
        // Parse function declarations
        java.util.List<ast.FunctionDeclaration> funcDecls = new java.util.ArrayList<>();
        while (have(NonTerminal.FUNC_DECL)) {
            funcDecls.add(funcDecl());
        }
        
        expect(Token.Kind.OPEN_BRACE);
        ast.StatementSequence statSeq = statSeq();
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.PERIOD);
        
        return new ast.Computation(lineNumber(), charPosition(), mainSymbol, varDecls, funcDecls, statSeq);
    }

    // --- IR/Optimization hooks -------------------------------------------------

    /**
     * Placeholder IR generation to keep pipeline compiling.
     * TODO: wire AST -> IRProgram/IRFunction/CFG construction.
     */
    public IRProgram genIR(AST ast) {
        return new IRGenerator().generate(ast);
    }

    /**
     * Run optimization pipeline and return DOT for entry function(s).
     */
    public String optimization(List<String> opts, boolean loop, boolean max) {
        IRProgram program = genIR(parsedAST);
        Optimizer optimizer = new Optimizer();
        optimizer.run(program, opts);
        return program.toDot();
    }
}
