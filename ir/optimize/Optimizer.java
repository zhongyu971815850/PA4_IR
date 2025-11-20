package ir.optimize;

import java.util.List;

import ir.IRFunction;
import ir.IRProgram;
import ir.optimize.passes.CSEPass;
import ir.optimize.passes.CFGConstPropPass;
import ir.optimize.passes.ConstantPropagationPass;
import ir.optimize.passes.GlobalDCEPass;
import ir.optimize.passes.CopyPropagationLocalPass;

/**
 * Runs a fixed sequence of local optimizations.
 */
public class Optimizer {

    private final ConstantPropagationPass cp = new ConstantPropagationPass();
    private final CSEPass cse = new CSEPass();
    private final GlobalDCEPass dce = new GlobalDCEPass();
    private final CFGConstPropPass cfgCP = new CFGConstPropPass();
    private final CopyPropagationLocalPass localCopy = new CopyPropagationLocalPass();

    public void run(IRProgram program, List<String> enabled) {
        boolean runAll = enabled == null || enabled.isEmpty();
        boolean wantsLoop = enabled != null && enabled.contains("loop");
        int maxIter = wantsLoop ? 10 : 5;

        for (IRFunction fn : program.getFunctions()) {
            recomputeReachability(fn);
            boolean changed;
            int iterations = 0;
            do {
                changed = false;
                fn.getControlFlowGraph().resetVisited();
                for (var block : fn.getBlocks()) {
                    if (runAll || enabled.contains("cse")) {
                        changed |= cse.run(block);
                    }
                    if (runAll || enabled.contains("cpp")) {
                        changed |= localCopy.run(block);
                    }
                }
                if (runAll || enabled.contains("cp") || enabled.contains("cpp") || enabled.contains("cf")) {
                    changed |= cfgCP.run(fn.getControlFlowGraph());
                    recomputeReachability(fn);
                }
                if (runAll || enabled.contains("dce")) {
                    changed |= dce.run(fn.getControlFlowGraph(), program.getGlobalSymbols());
                }
                iterations++;
            } while (changed && iterations < maxIter);
        }
    }

    private void recomputeReachability(IRFunction fn) {
        fn.getControlFlowGraph().resetVisited();
        dfs(fn.getEntry());
        for (var block : fn.getBlocks()) {
            block.setUnreachable(!block.visited());
        }
        fn.getControlFlowGraph().pruneUnreachable();
    }

    private void dfs(ir.cfg.BasicBlock block) {
        if (block == null || block.visited()) return;
        block.markVisited();
        for (ir.cfg.BasicBlock succ : block.getSuccessors()) {
            dfs(succ);
        }
    }
}
