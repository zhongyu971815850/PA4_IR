package mocha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Parser {

    // Error Reporting ============================================================
    private StringBuilder errorBuffer = new StringBuilder();

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

    private int lineNumber () {
        return currentToken.lineNumber();
    }

    private int charPosition () {
        return currentToken.charPosition();
    }

// Parser ============================================================
    private Scanner scanner;
    private Token currentToken;

    private BufferedReader reader;
    private StringTokenizer st;

    // TODO: add maps from Token IDENT to int/float/bool

    public Parser (Scanner scanner, InputStream in) {
        this.scanner = scanner;
        currentToken = this.scanner.next();

        reader = new BufferedReader(new InputStreamReader(in));
        st = null;
    }

    public void parse () {
        try {
            computation();
        }
        catch (QuitParseException q) {
            // too verbose
            // errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            // errorBuffer.append("[Could not complete parsing.]");
        }
    }

// Helper Methods =============================================================
    private boolean have (Token.Kind kind) {
        return currentToken.kind == kind;
    }

    private boolean have (NonTerminal nt) {
        return nt.firstSet().contains(currentToken.kind);
    }

    private boolean accept (Token.Kind kind) {
        // System.out.println("accept: " + currentToken.kind + " " + kind);
        if (have(kind)) {
            try {
                currentToken = scanner.next();
            }
            catch (NoSuchElementException e) {
                if (!kind.equals(Token.Kind.EOF)) {
                    // System.out.println("accept error");
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
        // System.out.println("expect error" + currentToken.kind);
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
        // System.out.println("expectRetrieve error");
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

    // Operator parsing methods
    private boolean powOp() {
        return accept(Token.Kind.POW);
    }
    
    private boolean mulOp() {
        return accept(Token.Kind.MUL) || accept(Token.Kind.DIV) || 
               accept(Token.Kind.MOD) || accept(Token.Kind.AND);
    }
    
    private boolean addOp() {
        return accept(Token.Kind.ADD) || accept(Token.Kind.SUB) || 
               accept(Token.Kind.OR);
    }
    
    private boolean relOp() {
        return accept(Token.Kind.EQUAL_TO) || accept(Token.Kind.NOT_EQUAL) ||
               accept(Token.Kind.LESS_THAN) || accept(Token.Kind.LESS_EQUAL) ||
               accept(Token.Kind.GREATER_THAN) || accept(Token.Kind.GREATER_EQUAL);
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
    
    private boolean type() {
        return accept(Token.Kind.BOOL) || accept(Token.Kind.INT) || 
               accept(Token.Kind.FLOAT);
    }

    // literal = integerLit | floatLit
    private Token literal () {
        return matchNonTerminal(NonTerminal.LITERAL);
    }

    // designator = ident { "[" relExpr "]" }
    private void designator () {
        expect(Token.Kind.IDENT);
        
        // Handle array access: { "[" relExpr "]" }
        while (accept(Token.Kind.OPEN_BRACKET)) {
            relExpr();
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }

    // Expression parsing methods
    // groupExpr = literal | designator | "not" relExpr | relation | funcCall
    private void groupExpr() {
        if (have(NonTerminal.LITERAL)) {
            literal();
        } else if (have(NonTerminal.DESIGNATOR)) {
            designator();
        } else if (accept(Token.Kind.NOT)) {
            relExpr();
        } else if (have(NonTerminal.RELATION)) {
            relation();
        } else if (have(NonTerminal.FUNC_CALL)) {
            funcCall();
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.GROUP_EXPR);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // powExpr = groupExpr { powOp groupExpr }
    private void powExpr() {
        groupExpr();
        while (powOp()) {
            groupExpr();
        }
    }
    
    // multExpr = powExpr { multOp powExpr }
    private void multExpr() {
        powExpr();
        while (mulOp()) {
            powExpr();
        }
    }
    
    // addExpr = multExpr { addOp multExpr }
    private void addExpr() {
        multExpr();
        while (addOp()) {
            multExpr();
        }
    }
    
    // relExpr = addExpr { relOp addExpr }
    private void relExpr() {
        addExpr();
        while (relOp()) {
            addExpr();
        }
    }
    
    // relation = "(" relExpr ")"
    private void relation() {
        expect(Token.Kind.OPEN_PAREN);
        relExpr();
        expect(Token.Kind.CLOSE_PAREN);
    }
    
    // funcCall = "call" ident "(" [ relExpr { "," relExpr } ] ")"
    private void funcCall() {
        expect(Token.Kind.CALL);
        expect(Token.Kind.IDENT);
        expect(Token.Kind.OPEN_PAREN);
        
        // Optional parameters
        if (have(NonTerminal.REL_EXPR)) {
            relExpr();
            while (accept(Token.Kind.COMMA)) {
                relExpr();
            }
        }
        
        expect(Token.Kind.CLOSE_PAREN);
    }

    // Statement parsing methods
    // assign = designator ( ( assignOp relExpr ) | unaryOp )
    private void assign() {
        designator();
        
        if (assignOp()) {
            relExpr();
        } else if (unaryOp()) {
            // unaryOp already consumed
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.ASSIGN);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // ifStat = "if" relation "then" statSeq [ "else" statSeq ] "fi"
    private void ifStat() {
        expect(Token.Kind.IF);
        relation();
        expect(Token.Kind.THEN);
        statSeq();
        
        if (accept(Token.Kind.ELSE)) {
            statSeq();
        }
        
        expect(Token.Kind.FI);
    }
    
    // whileStat = "while" relation "do" statSeq "od"
    private void whileStat() {
        expect(Token.Kind.WHILE);
        relation();
        expect(Token.Kind.DO);
        statSeq();
        expect(Token.Kind.OD);
    }
    
    // repeatStat = "repeat" statSeq "until" relation
    private void repeatStat() {
        expect(Token.Kind.REPEAT);
        statSeq();
        expect(Token.Kind.UNTIL);
        relation();
    }
    
    // returnStat = "return" [ relExpr ]
    private void returnStat() {
        expect(Token.Kind.RETURN);
        if (have(NonTerminal.REL_EXPR)) {
            relExpr();
        }
    }
    
    // statement = assign | funcCall | ifStat | whileStat | repeatStat | returnStat
    private void statement() {
        if (have(NonTerminal.ASSIGN)) {
            assign();
        } else if (have(NonTerminal.FUNC_CALL)) {
            funcCall();
        } else if (have(NonTerminal.IF_STAT)) {
            ifStat();
        } else if (have(NonTerminal.WHILE_STAT)) {
            whileStat();
        } else if (have(NonTerminal.REPEAT_STAT)) {
            repeatStat();
        } else if (have(NonTerminal.RETURN_STAT)) {
            returnStat();
        } else {
            String errorMessage = reportSyntaxError(NonTerminal.STATEMENT);
            throw new QuitParseException(errorMessage);
        }
    }
    
    // statSeq = statement ";" { statement ";" }
    private void statSeq() {
        statement();
        expect(Token.Kind.SEMICOLON);
        
        while (have(NonTerminal.STATEMENT)) {
            statement();
            expect(Token.Kind.SEMICOLON);
        }
    }

    // Declaration parsing methods
    // typeDecl = type { "[" integerLit "]" }
    private void typeDecl() {
        type();
        
        // Handle array dimensions: { "[" integerLit "]" }
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.INT_VAL);
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }
    
    // varDecl = typeDecl ident { "," ident } ";"
    private void varDecl() {
        typeDecl();
        expect(Token.Kind.IDENT);
        
        // Handle multiple identifiers: { "," ident }
        while (accept(Token.Kind.COMMA)) {
            expect(Token.Kind.IDENT);
        }
        
        expect(Token.Kind.SEMICOLON);
    }
    
    // paramType = type { "[" "]" }
    private void paramType() {
        type();
        
        // Handle array parameter: { "[" "]" }
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }
    
    // paramDecl = paramType ident
    private void paramDecl() {
        paramType();
        expect(Token.Kind.IDENT);
    }
    
    // formalParam = "(" [ paramDecl { "," paramDecl } ] ")"
    private void formalParam() {
        expect(Token.Kind.OPEN_PAREN);
        
        // Optional parameters
        if (have(NonTerminal.PARAM_DECL)) {
            paramDecl();
            while (accept(Token.Kind.COMMA)) {
                paramDecl();
            }
        }
        
        expect(Token.Kind.CLOSE_PAREN);
    }
    
    // funcBody = "{" { varDecl } statSeq "}" ";"
    private void funcBody() {
        expect(Token.Kind.OPEN_BRACE);
        
        // Handle variable declarations: { varDecl }
        while (have(NonTerminal.VAR_DECL)) {
            varDecl();
        }
        
        statSeq();
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.SEMICOLON);
    }
    
    // funcDecl = "function" ident formalParam ":" ( "void" | type ) funcBody
    private void funcDecl() {
        expect(Token.Kind.FUNC);
        expect(Token.Kind.IDENT);
        formalParam();
        expect(Token.Kind.COLON);
        
        // Return type: "void" | type
        if (accept(Token.Kind.VOID)) {
            // void return type
        } else if (type()) {
            // type return type
        } else {
            // System.out.println("funcDecl error");
            String errorMessage = reportSyntaxError(Token.Kind.VOID);
            throw new QuitParseException(errorMessage);
        }
        
        funcBody();
    }

    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private void computation () {
        
        expect(Token.Kind.MAIN);

        // deal with varDecl
        while (have(NonTerminal.VAR_DECL)) {
            varDecl();
        }
        
        // deal with funcDecl
        while (have(NonTerminal.FUNC_DECL)) {
            funcDecl();
        }

        expect(Token.Kind.OPEN_BRACE);
        statSeq();
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.PERIOD);       
    }
}
