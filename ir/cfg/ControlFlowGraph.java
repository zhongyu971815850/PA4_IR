package ir.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ControlFlowGraph implements Visitable {

    private final List<BasicBlock> blocks;
    private BasicBlock entry;

    public ControlFlowGraph() {
        this.blocks = new ArrayList<>();
    }

    public ControlFlowGraph(BasicBlock entry) {
        this();
        setEntry(entry);
    }

    public void addBlock(BasicBlock block) {
        if (block != null && !blocks.contains(block)) {
            blocks.add(block);
        }
    }

    public List<BasicBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public BasicBlock getEntry() {
        return entry;
    }

    public void setEntry(BasicBlock entry) {
        this.entry = Objects.requireNonNull(entry, "Entry block cannot be null");
        addBlock(entry);
    }

    @Override
    public void accept(CFGVisitor visitor) {
        visitor.visit(this);
    }

    public void resetVisited() {
        for (BasicBlock block : blocks) {
            block.resetVisited();
        }
    }

    public String asDotGraph() {
        return new CFGPrinter().print(this);
    }
}
