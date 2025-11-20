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

    /**
     * Remove blocks marked as unreachable from the CFG.
     */
    public void pruneUnreachable() {
        List<BasicBlock> toRemove = new ArrayList<>();
        for (BasicBlock b : blocks) {
            if (b.isUnreachable() && b != entry) {
                toRemove.add(b);
            }
        }
        if (toRemove.isEmpty()) return;
        blocks.removeAll(toRemove);
        // drop edges pointing to removed blocks
        for (BasicBlock b : blocks) {
            List<BasicBlock> succCopy = new ArrayList<>(b.getSuccessors());
            for (BasicBlock s : succCopy) {
                if (s.isUnreachable()) {
                    b.removeSuccessor(s);
                }
            }
            List<BasicBlock> predCopy = new ArrayList<>(b.getPredecessors());
            for (BasicBlock p : predCopy) {
                if (p.isUnreachable()) {
                    b.removePredecessor(p);
                }
            }
            // prune branch instructions to unreachable targets
            List<ir.tac.TAC> instrs = b.mutableInstructions();
            for (ir.tac.TAC tac : instrs) {
                if (tac instanceof ir.tac.Branch br && br.target().isUnreachable()) {
                    br.disableEmit();
                } else if (tac instanceof ir.tac.ConditionalBranch cb) {
                    if (cb.trueTarget() != null && cb.trueTarget().isUnreachable()) {
                        b.removeSuccessor(cb.trueTarget());
                    }
                    if (cb.falseTarget() != null && cb.falseTarget().isUnreachable()) {
                        b.removeSuccessor(cb.falseTarget());
                    }
                }
            }
        }
    }

    public String asDotGraph() {
        return new CFGPrinter().print(this, "CFG");
    }
}
