package ast;

import java.util.List;
import mocha.Symbol;

public class Computation extends Node {

    private Symbol main;
    private DeclarationList vars;
    private List<FunctionDeclaration> funcs;
    private StatementSequence mainSeq;

    public Computation(int lineNum, int charPos, Symbol main, DeclarationList vars, List<FunctionDeclaration> funcs,
            StatementSequence mainSeq) {
        super(lineNum, charPos);
        this.main = main;
        this.vars = vars;
        this.funcs = funcs;
        this.mainSeq = mainSeq;
    }

    public Symbol main() {
        return main;
    }

    public DeclarationList variables() {
        return vars;
    }

    public List<FunctionDeclaration> functions() {
        return funcs;
    }

    public StatementSequence mainStatementSequence() {
        return mainSeq;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
