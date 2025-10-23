package ast;

import mocha.Token;

public class Power extends Node implements Expression {
    private Expression left;
    private Expression right;
    private Token operator;

    public Power(int lineNum, int charPos, Expression left, Token operator, Expression right) {
        super(lineNum, charPos);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public Expression left() {
        return left;
    }

    public Expression right() {
        return right;
    }

    public Token operator() {
        return operator;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
