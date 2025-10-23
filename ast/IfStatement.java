package ast;

public class IfStatement extends Node implements Statement {
    private Expression condition;
    private StatementSequence thenBranch;
    private StatementSequence elseBranch;

    public IfStatement(int lineNum, int charPos, Expression condition, StatementSequence thenBranch, StatementSequence elseBranch) {
        super(lineNum, charPos);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Expression condition() {
        return condition;
    }

    public StatementSequence thenBranch() {
        return thenBranch;
    }

    public StatementSequence elseBranch() {
        return elseBranch;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
