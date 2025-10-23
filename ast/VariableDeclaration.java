package ast;

import java.util.List;
import mocha.Symbol;
import types.Type;

public class VariableDeclaration extends Node {
    private Type type;
    private List<Symbol> variables;

    public VariableDeclaration(int lineNum, int charPos, Type type, List<Symbol> variables) {
        super(lineNum, charPos);
        this.type = type;
        this.variables = variables;
    }

    public Type type() {
        return type;
    }

    public List<Symbol> variables() {
        return variables;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
