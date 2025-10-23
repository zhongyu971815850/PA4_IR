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

    public BasicBlock(int num) {
        this.num = num;
        this.instructions = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.dominators = new LinkedHashSet<>();
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

    public void addSuccessor(BasicBlock successor) {
        if (successor == null || successors.contains(successor)) {
            return;
        }
        successors.add(successor);
        successor.addPredecessor(this);
    }

    private void addPredecessor(BasicBlock predecessor) {
        if (predecessor == null || predecessors.contains(predecessor)) {
            return;
        }
        predecessors.add(predecessor);
    }

    public List<BasicBlock> getPredecessors() {
        return Collections.unmodifiableList(predecessors);
    }

    public List<BasicBlock> getSuccessors() {
        return Collections.unmodifiableList(successors);
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
