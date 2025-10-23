package ast;

import mocha.Token;

public class FloatLiteral extends Node implements Expression {
    private Token token;
    private double value;

    public FloatLiteral(int lineNum, int charPos, Token token) {
        super(lineNum, charPos);
        this.token = token;
        this.value = Double.parseDouble(token.lexeme());
    }

    public Token token() {
        return token;
    }

    public double value() {
        return value;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
