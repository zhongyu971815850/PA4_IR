package ast;

import mocha.Token;

public class LogicalNot extends Node implements Expression {
    private Expression operand;
    private Token operator;

    public LogicalNot(int lineNum, int charPos, Token operator, Expression operand) {
        super(lineNum, charPos);
        this.operand = operand;
        this.operator = operator;
    }

    public Expression operand() {
        return operand;
    }

    public Token operator() {
        return operator;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
