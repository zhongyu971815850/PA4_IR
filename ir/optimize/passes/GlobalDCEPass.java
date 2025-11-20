package ir.optimize.passes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ir.cfg.BasicBlock;
import ir.cfg.ControlFlowGraph;
import ir.tac.Assign;
import ir.tac.Call;
import ir.tac.ConditionalBranch;
import ir.tac.Read;
import ir.tac.Return;
import ir.tac.Store;
import ir.tac.TAC;
import ir.tac.Value;
import ir.tac.ValueList;
import ir.tac.Variable;
import ir.tac.Write;

/**
 * Dead code elimination using backward live-variable analysis across the CFG.
 */
public class GlobalDCEPass {

    public boolean run(ControlFlowGraph cfg, java.util.Set<mocha.Symbol> globalSymbols) {
        LiveVariableAnalysis lva = new LiveVariableAnalysis(cfg);
        var result = lva.analyze();
        boolean changed = false;
        for (BasicBlock block : cfg.getBlocks()) {
            if (block.isUnreachable()) continue;
            pruneAfterTerminator(block);
            Set<Variable> live = new HashSet<>(result.out().getOrDefault(block, Set.of()));
            List<TAC> instrs = block.mutableInstructions();
            for (int i = instrs.size() - 1; i >= 0; i--) {
                TAC tac = instrs.get(i);
                if (tac instanceof Assign assign) {
                    addUses(assign.leftOperand(), live);
                    addUses(assign.rightOperand(), live);
                    Variable def = assign.destination();
                    boolean needed = def != null && live.contains(def);
                    if (!hasSideEffect(tac) && def != null && !needed) {
                        assign.disableEmit();
                        changed = true;
                    } else if (def != null) {
                        live.remove(def);
                        live.add(def);
                    }
                } else if (tac instanceof ConditionalBranch cb) {
                    addUses(cb.left(), live);
                    addUses(cb.right(), live);
                } else if (tac instanceof Return ret) {
                    addUses(ret.value(), live);
                } else if (tac instanceof Write write) {
                    addUses(write.value(), live);
                } else if (tac instanceof Call call) {
                    addUses(call.arguments(), live);
                    if (call.hasDestination()) {
                        live.remove(call.destination());
                        live.add(call.destination());
                    }
                } else if (tac instanceof Read read) {
                    // read defines destination
                    live.remove(read.destination());
                    live.add(read.destination());
                }
            }
        }
        return changed;
    }

    private void pruneAfterTerminator(BasicBlock block) {
        boolean terminated = false;
        for (TAC tac : block.mutableInstructions()) {
            if (terminated) {
                tac.disableEmit();
                continue;
            }
            if (tac instanceof Return || tac instanceof ir.tac.Branch || tac instanceof ir.tac.ConditionalBranch) {
                terminated = true;
            }
        }
    }

    private void addUses(Value v, Set<Variable> live) {
        if (v instanceof Variable var) {
            live.add(var);
        }
    }

    private void addUses(ValueList list, Set<Variable> live) {
        if (list == null) return;
        for (Value v : list) {
            addUses(v, live);
        }
    }

    private boolean hasSideEffect(TAC tac) {
        return tac instanceof Call
                || tac instanceof Write
                || tac instanceof Read
                || tac instanceof Store;
    }
}
