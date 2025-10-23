package ast;

import mocha.Token;

public class BoolLiteral extends Node implements Expression {
    private Token token;
    private boolean value;

    public BoolLiteral(int lineNum, int charPos, Token token) {
        super(lineNum, charPos);
        this.token = token;
        this.value = token.lexeme().equals("true");
    }

    public Token token() {
        return token;
    }

    public boolean value() {
        return value;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
