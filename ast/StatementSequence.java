package ast;

import java.util.ArrayList;
import java.util.List;

public class StatementSequence extends Node {
    private List<Statement> statements;

    public StatementSequence(int lineNum, int charPos) {
        super(lineNum, charPos);
        this.statements = new ArrayList<>();
    }

    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    public List<Statement> statements() {
        return statements;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
