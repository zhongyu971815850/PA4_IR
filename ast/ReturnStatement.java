package ast;

public class ReturnStatement extends Node implements Statement {
    private Expression expression;

    public ReturnStatement(int lineNum, int charPos, Expression expression) {
        super(lineNum, charPos);
        this.expression = expression;
    }

    public Expression expression() {
        return expression;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
