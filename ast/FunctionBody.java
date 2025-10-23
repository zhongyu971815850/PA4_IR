package ast;

import java.util.ArrayList;
import java.util.List;

public class FunctionBody extends Node {
    private List<VariableDeclaration> variables;
    private StatementSequence statements;

    public FunctionBody(int lineNum, int charPos) {
        super(lineNum, charPos);
        this.variables = new ArrayList<>();
        this.statements = new StatementSequence(lineNum, charPos);
    }

    public void addVariable(VariableDeclaration variable) {
        variables.add(variable);
    }

    public List<VariableDeclaration> variables() {
        return variables;
    }

    public StatementSequence statements() {
        return statements;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
