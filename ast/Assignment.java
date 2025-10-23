package ast;

import mocha.Token;

public class Assignment extends Node implements Statement {
    private Expression destination;
    private Expression source;
    private Token operator;

    public Assignment(int lineNum, int charPos, Expression destination, Token operator, Expression source) {
        super(lineNum, charPos);
        this.destination = destination;
        this.source = source;
        this.operator = operator;
    }

    public Expression destination() {
        return destination;
    }

    public Expression source() {
        return source;
    }

    public Token operator() {
        return operator;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
