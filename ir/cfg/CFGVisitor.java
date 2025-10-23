package ir.cfg;

public interface CFGVisitor {
    void visit(ControlFlowGraph graph);
    void visit(BasicBlock block);
}
