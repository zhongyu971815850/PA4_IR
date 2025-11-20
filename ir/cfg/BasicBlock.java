package ir.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ir.tac.TAC;

public class BasicBlock extends Block implements Iterable<TAC> {

    private final int num; // block number
    private final List<TAC> instructions;
    private final List<BasicBlock> predecessors;
    private final List<BasicBlock> successors;
    private final Set<BasicBlock> dominators;
    private BasicBlock immediateDominator;
    private boolean unreachable;

    public BasicBlock(int num) {
        this.num = num;
        this.instructions = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.dominators = new LinkedHashSet<>();
        this.unreachable = false;
    }

    public int getNumber() {
        return num;
    }

    public String getLabel() {
        return "BB" + num;
    }

    public void addInstruction(TAC tac) {
        instructions.add(Objects.requireNonNull(tac, "Instruction cannot be null"));
    }

    public List<TAC> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Internal mutable view of the instruction list for optimization passes.
     */
    public List<TAC> mutableInstructions() {
        return instructions;
    }

    public void replaceInstruction(int index, TAC tac) {
        instructions.set(index, Objects.requireNonNull(tac, "Instruction cannot be null"));
    }

    public void addSuccessor(BasicBlock successor) {
        if (successor == null || successors.contains(successor)) {
            return;
        }
        successors.add(successor);
        successor.addPredecessor(this);
    }

    public void clearSuccessors() {
        for (BasicBlock succ : new ArrayList<>(successors)) {
            succ.removePredecessor(this);
        }
        successors.clear();
    }

    public void setSuccessors(List<BasicBlock> newSucc) {
        clearSuccessors();
        if (newSucc != null) {
            for (BasicBlock succ : newSucc) {
                addSuccessor(succ);
            }
        }
    }

    private void addPredecessor(BasicBlock predecessor) {
        if (predecessor == null || predecessors.contains(predecessor)) {
            return;
        }
        predecessors.add(predecessor);
    }

    public void removePredecessor(BasicBlock predecessor) {
        predecessors.remove(predecessor);
    }

    public List<BasicBlock> getPredecessors() {
        return Collections.unmodifiableList(predecessors);
    }

    public List<BasicBlock> getSuccessors() {
        return Collections.unmodifiableList(successors);
    }

    public void removeSuccessor(BasicBlock succ) {
        successors.remove(succ);
    }

    public void setImmediateDominator(BasicBlock immediateDominator) {
        this.immediateDominator = immediateDominator;
    }

    public BasicBlock getImmediateDominator() {
        return immediateDominator;
    }

    public void setDominators(Set<BasicBlock> dominators) {
        this.dominators.clear();
        if (dominators != null) {
            this.dominators.addAll(dominators);
        }
    }

    public void addDominator(BasicBlock block) {
        if (block != null) {
            dominators.add(block);
        }
    }

    public Set<BasicBlock> getDominators() {
        return Collections.unmodifiableSet(dominators);
    }

    public boolean isUnreachable() {
        return unreachable;
    }

    public void setUnreachable(boolean unreachable) {
        this.unreachable = unreachable;
    }

    @Override
    public Iterator<TAC> iterator() {
        return instructions.iterator();
    }

    @Override
    public void accept(CFGVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void resetVisited() {
        visited = false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getLabel()).append(':');
        for (TAC tac : instructions) {
            builder.append("\n  ").append(tac);
        }
        return builder.toString();
    }
}
