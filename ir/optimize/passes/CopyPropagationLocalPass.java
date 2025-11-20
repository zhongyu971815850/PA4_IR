package ir.optimize.passes;

import java.util.HashMap;
import java.util.Map;

import ir.cfg.BasicBlock;
import ir.tac.Assign;
import ir.tac.Copy;
import ir.tac.TAC;
import ir.tac.Value;
import ir.tac.Variable;

/**
 * Simple intra-block copy propagation to collapse copy chains.
 */
public class CopyPropagationLocalPass {

    public boolean run(BasicBlock block) {
        if (block.isUnreachable()) return false;
        Map<Variable, Value> map = new HashMap<>();
        boolean changed = false;
        for (int i = 0; i < block.getInstructions().size(); i++) {
            TAC tac = block.getInstructions().get(i);
            if (tac instanceof Assign assign) {
                Value left = resolve(assign.leftOperand(), map);
                Value right = resolve(assign.rightOperand(), map);
                Assign rewritten = rebuild(assign, left, right);
                if (rewritten != assign) {
                    block.replaceInstruction(i, rewritten);
                    assign = rewritten;
                    changed = true;
                }
                map.remove(assign.destination());
                if (assign instanceof Copy) {
                    map.put(assign.destination(), resolve(assign.leftOperand(), map));
                }
            } else if (tac instanceof ir.tac.Call call) {
                ir.tac.ValueList newArgs = new ir.tac.ValueList();
                for (Value v : call.arguments()) {
                    newArgs.add(resolve(v, map));
                }
                if (!newArgs.asList().equals(call.arguments().asList())) {
                    if (call.hasDestination()) {
                        block.replaceInstruction(i, new ir.tac.Call(call.id(), call.destination(), call.function(), newArgs));
                    } else {
                        block.replaceInstruction(i, new ir.tac.Call(call.id(), call.function(), newArgs));
                    }
                    changed = true;
                }
                if (call.hasDestination()) {
                    map.remove(call.destination());
                }
            }
        }
        return changed;
    }

    private Value resolve(Value v, Map<Variable, Value> map) {
        if (v instanceof Variable var && map.containsKey(var)) {
            Value target = map.get(var);
            if (target instanceof Variable targVar && !targVar.equals(var)) {
                return resolve(target, map);
            }
            return target;
        }
        return v;
    }

    private Assign rebuild(Assign original, Value left, Value right) {
        if (original instanceof Copy) {
            if (!left.equals(original.leftOperand())) {
                return new Copy(original.id(), original.destination(), left);
            }
            return original;
        }
        if (!left.equals(original.leftOperand()) || (right != null && !right.equals(original.rightOperand()))) {
            if (original instanceof ir.tac.Add) {
                return new ir.tac.Add(original.id(), original.destination(), left, right);
            } else if (original instanceof ir.tac.Sub) {
                return new ir.tac.Sub(original.id(), original.destination(), left, right);
            } else if (original instanceof ir.tac.Mul) {
                return new ir.tac.Mul(original.id(), original.destination(), left, right);
            } else if (original instanceof ir.tac.Div) {
                return new ir.tac.Div(original.id(), original.destination(), left, right);
            }
        }
        return original;
    }
}
