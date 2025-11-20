package ir.tac;

public abstract class TAC implements Visitable {

    private final int id; // instruction id
    /**
     * Whether this instruction should be emitted when generating DLX.
     * Optimizations mark instructions as non-emitting instead of removing them
     * so that later passes can still reason about CFG shape.
     */
    private boolean emit;

    protected TAC(int id) {
        this.id = id;
        this.emit = true;
    }

    public int id() {
        return id;
    }

    /**
     * Legacy helper kept for compatibility with earlier passes.
     * Returns true when the instruction has been disabled.
     */
    public boolean isEliminated() {
        return !emit;
    }

    public boolean shouldEmit() {
        return emit;
    }

    public void disableEmit() {
        emit = false;
    }

    public void enableEmit() {
        emit = true;
    }

    /**
     * Backwards compatibility shims for older optimization code.
     */
    public void markEliminated() {
        disableEmit();
    }

    public void clearEliminated() {
        enableEmit();
    }

    @Override
    public abstract String toString();
}
