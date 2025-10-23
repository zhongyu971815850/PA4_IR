package ir.tac;

/**
 * Base class for three-address-code (TAC) instructions used by the SSA IR.
 * Each instruction has a unique identifier which is preserved for debugging
 * and for mapping back to source locations. Instructions can be marked as
 * eliminated by optimizations without being removed from their block so that
 * subsequent passes can still reason about the original control-flow shape.
 */
public abstract class TAC implements Visitable {

    private final int id; // instruction id

    private boolean eliminated; // if this instruction is not needed by any optimization

    protected TAC(int id) {
        this.id = id;
        this.eliminated = false;
    }

    public int id() {
        return id;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void markEliminated() {
        eliminated = true;
    }

    public void clearEliminated() {
        eliminated = false;
    }

    @Override
    public abstract String toString();
}
