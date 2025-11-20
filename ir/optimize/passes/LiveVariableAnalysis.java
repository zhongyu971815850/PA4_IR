package ir.optimize.passes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ir.cfg.BasicBlock;
import ir.cfg.ControlFlowGraph;
import ir.optimize.dataflow.DataFlowAnalysis;
import ir.tac.Assign;
import ir.tac.Call;
import ir.tac.ConditionalBranch;
import ir.tac.Read;
import ir.tac.Return;
import ir.tac.TAC;
import ir.tac.Value;
import ir.tac.ValueList;
import ir.tac.Variable;
import ir.tac.Write;

/**
 * Backward live-variable analysis across the CFG.
 */
public class LiveVariableAnalysis extends DataFlowAnalysis<Set<Variable>> {

    private final Map<BasicBlock, Set<Variable>> use = new HashMap<>();
    private final Map<BasicBlock, Set<Variable>> def = new HashMap<>();

    public LiveVariableAnalysis(ControlFlowGraph cfg) {
        super(cfg, Direction.BACKWARD, set -> new HashSet<>(set));
        computeUseDef(cfg);
    }

    @Override
    protected BasicBlock boundaryBlock() {
        return null; // no special boundary; use standard successor propagation
    }

    private void computeUseDef(ControlFlowGraph cfg) {
        for (BasicBlock block : cfg.getBlocks()) {
            Set<Variable> useSet = new HashSet<>();
            Set<Variable> defSet = new HashSet<>();
            for (TAC tac : block.getInstructions()) {
                if (tac instanceof Assign assign) {
                    addUse(assign.leftOperand(), useSet, defSet);
                    addUse(assign.rightOperand(), useSet, defSet);
                    defSet.add(assign.destination());
                } else if (tac instanceof Call call) {
                    addUse(call.arguments(), useSet, defSet);
                    if (call.hasDestination()) {
                        defSet.add(call.destination());
                    }
                } else if (tac instanceof ConditionalBranch cb) {
                    addUse(cb.left(), useSet, defSet);
                    addUse(cb.right(), useSet, defSet);
                } else if (tac instanceof Return ret) {
                    addUse(ret.value(), useSet, defSet);
                } else if (tac instanceof Write write) {
                    addUse(write.value(), useSet, defSet);
                } else if (tac instanceof Read read) {
                    defSet.add(read.destination());
                }
            }
            use.put(block, useSet);
            def.put(block, defSet);
        }
    }

    private void addUse(Value v, Set<Variable> useSet, Set<Variable> defSet) {
        if (v instanceof Variable var && !defSet.contains(var)) {
            useSet.add(var);
        }
    }

    private void addUse(ValueList list, Set<Variable> useSet, Set<Variable> defSet) {
        if (list == null) return;
        for (Value v : list) {
            addUse(v, useSet, defSet);
        }
    }

    @Override
    protected Set<Variable> top() {
        return new HashSet<>();
    }

    @Override
    protected Set<Variable> boundary() {
        return new HashSet<>();
    }

    @Override
    protected Set<Variable> transfer(BasicBlock block, Set<Variable> input) {
        // input corresponds to out set in backward analysis
        Set<Variable> out = new HashSet<>(input);
        Set<Variable> in = new HashSet<>(use.getOrDefault(block, Set.of()));
        Set<Variable> defs = def.getOrDefault(block, Set.of());
        for (Variable v : out) {
            if (!defs.contains(v)) {
                in.add(v);
            }
        }
        return in;
    }

    @Override
    protected Set<Variable> meet(Set<Variable> left, Set<Variable> right) {
        Set<Variable> res = new HashSet<>();
        res.addAll(left);
        res.addAll(right);
        return res;
    }
}
