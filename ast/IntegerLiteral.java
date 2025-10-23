package ast;

import mocha.Token;

public class IntegerLiteral extends Node implements Expression {
    private Token token;
    private int value;

    public IntegerLiteral(int lineNum, int charPos, Token token) {
        super(lineNum, charPos);
        this.token = token;
        this.value = Integer.parseInt(token.lexeme());
    }

    public Token token() {
        return token;
    }

    public int value() {
        return value;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
