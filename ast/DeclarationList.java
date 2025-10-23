package ast;

import java.util.ArrayList;
import java.util.List;

public class DeclarationList extends Node {
    private List<VariableDeclaration> declarations;

    public DeclarationList(int lineNum, int charPos) {
        super(lineNum, charPos);
        this.declarations = new ArrayList<>();
    }

    public void addDeclaration(VariableDeclaration declaration) {
        declarations.add(declaration);
    }

    public List<VariableDeclaration> declarations() {
        return declarations;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
