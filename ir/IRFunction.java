package ir;

import java.util.List;
import java.util.Objects;

import ir.cfg.BasicBlock;
import ir.cfg.ControlFlowGraph;
import mocha.Symbol;

/**
 * IR for a single function or the program entry point.
 */
public class IRFunction {

    private final Symbol symbol;
    private final ControlFlowGraph cfg;
    private BasicBlock entry;
    private BasicBlock exit;

    public IRFunction(Symbol symbol) {
        this(symbol, new ControlFlowGraph());
    }

    public IRFunction(Symbol symbol, ControlFlowGraph cfg) {
        this.symbol = Objects.requireNonNull(symbol, "Function symbol cannot be null");
        this.cfg = Objects.requireNonNull(cfg, "Control-flow graph cannot be null");
    }

    public String getName() {
        return symbol.name();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public ControlFlowGraph getControlFlowGraph() {
        return cfg;
    }

    public BasicBlock getEntry() {
        return entry;
    }

    public void setEntry(BasicBlock entry) {
        this.entry = Objects.requireNonNull(entry, "Entry block cannot be null");
        cfg.setEntry(entry);
    }

    public BasicBlock getExit() {
        return exit;
    }

    public void setExit(BasicBlock exit) {
        this.exit = Objects.requireNonNull(exit, "Exit block cannot be null");
        cfg.addBlock(exit);
    }

    public void addBlock(BasicBlock block) {
        cfg.addBlock(block);
    }

    public List<BasicBlock> getBlocks() {
        return cfg.getBlocks();
    }

    public String toDot() {
        return cfg.asDotGraph();
    }
}
