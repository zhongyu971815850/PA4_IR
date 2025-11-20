package ir.optimize.dataflow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import ir.cfg.BasicBlock;
import ir.cfg.ControlFlowGraph;

/**
 * Generic worklist-based data-flow analysis framework.
 *
 * @param <F> lattice element type
 */
public abstract class DataFlowAnalysis<F> {

    public enum Direction {
        FORWARD, BACKWARD
    }

    private final ControlFlowGraph cfg;
    private final Direction direction;
    private final Function<F, F> copyFn;

    protected DataFlowAnalysis(ControlFlowGraph cfg, Direction direction) {
        this(cfg, direction, null);
    }

    /**
     * @param copyFn optional copier for mutable lattice elements
     */
    protected DataFlowAnalysis(ControlFlowGraph cfg, Direction direction, Function<F, F> copyFn) {
        this.cfg = Objects.requireNonNull(cfg, "CFG cannot be null");
        this.direction = Objects.requireNonNull(direction, "Direction cannot be null");
        this.copyFn = copyFn;
    }

    protected abstract F top();

    protected abstract F boundary();

    protected abstract F transfer(BasicBlock block, F input);

    protected abstract F meet(F left, F right);

    protected BasicBlock boundaryBlock() {
        return cfg.getEntry();
    }

    private F copy(F value) {
        return copyFn != null ? copyFn.apply(value) : value;
    }

    public Result<F> analyze() {
        Map<BasicBlock, F> in = new HashMap<>();
        Map<BasicBlock, F> out = new HashMap<>();

        List<BasicBlock> blocks = cfg.getBlocks();
        for (BasicBlock block : blocks) {
            in.put(block, copy(top()));
            out.put(block, copy(top()));
        }

        Deque<BasicBlock> worklist = new ArrayDeque<>(blocks);

        while (!worklist.isEmpty()) {
            BasicBlock block = worklist.removeFirst();
            boolean changed;
            switch (direction) {
                case FORWARD:
                    changed = processForward(block, in, out);
                    if (changed) {
                        worklist.addAll(block.getSuccessors());
                    }
                    break;
                case BACKWARD:
                    changed = processBackward(block, in, out);
                    if (changed) {
                        worklist.addAll(block.getPredecessors());
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown direction " + direction);
            }
        }

        return new Result<>(in, out);
    }

    private boolean processForward(BasicBlock block, Map<BasicBlock, F> in, Map<BasicBlock, F> out) {
        F accumulated = null;
        for (BasicBlock pred : block.getPredecessors()) {
            accumulated = accumulated == null ? copy(out.get(pred)) : meet(accumulated, out.get(pred));
        }
        if (accumulated == null) {
            accumulated = copy(top());
        }
        if (block.equals(boundaryBlock())) {
            accumulated = copy(boundary());
        }

        boolean changed = !Objects.equals(in.get(block), accumulated);
        if (changed) {
            in.put(block, accumulated);
        }

        F transferred = transfer(block, copy(accumulated));
        if (!Objects.equals(out.get(block), transferred)) {
            out.put(block, transferred);
            changed = true;
        }
        return changed;
    }

    private boolean processBackward(BasicBlock block, Map<BasicBlock, F> in, Map<BasicBlock, F> out) {
        F accumulated = null;
        for (BasicBlock succ : block.getSuccessors()) {
            accumulated = accumulated == null ? copy(in.get(succ)) : meet(accumulated, in.get(succ));
        }
        if (accumulated == null) {
            accumulated = copy(top());
        }
        if (block.equals(boundaryBlock())) {
            accumulated = copy(boundary());
        }

        boolean changed = !Objects.equals(out.get(block), accumulated);
        if (changed) {
            out.put(block, accumulated);
        }

        F transferred = transfer(block, copy(accumulated));
        if (!Objects.equals(in.get(block), transferred)) {
            in.put(block, transferred);
            changed = true;
        }
        return changed;
    }

    public static final class Result<F> {
        private final Map<BasicBlock, F> in;
        private final Map<BasicBlock, F> out;

        private Result(Map<BasicBlock, F> in, Map<BasicBlock, F> out) {
            this.in = in;
            this.out = out;
        }

        public Map<BasicBlock, F> in() {
            return in;
        }

        public Map<BasicBlock, F> out() {
            return out;
        }
    }
}
