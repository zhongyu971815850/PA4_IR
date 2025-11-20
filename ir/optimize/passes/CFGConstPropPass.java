package ir.optimize.passes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ir.cfg.BasicBlock;
import ir.cfg.ControlFlowGraph;
import ir.optimize.ValueUtils;
import ir.tac.Add;
import ir.tac.Assign;
import ir.tac.Branch;
import ir.tac.ConditionalBranch;
import ir.tac.Copy;
import ir.tac.Div;
import ir.tac.Literal;
import ir.tac.Mul;
import ir.tac.Sub;
import ir.tac.TAC;
import ir.tac.Value;
import ir.tac.Variable;

/**
 * Forward constant + copy propagation with folding and branch simplification over the CFG.
 */
public class CFGConstPropPass {

    private enum Kind { UNDEF, CONST, COPY, TOP }

    private static final class Lattice {
        final Kind kind;
        final Value constVal;
        final Variable copyOf;

        Lattice(Kind kind, Value constVal, Variable copyOf) {
            this.kind = kind;
            this.constVal = constVal;
            this.copyOf = copyOf;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Lattice l)) return false;
            return kind == l.kind && Objects.equals(constVal, l.constVal) && Objects.equals(copyOf, l.copyOf);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, constVal, copyOf);
        }

        static Lattice undef() { return new Lattice(Kind.UNDEF, null, null); }
        static Lattice top() { return new Lattice(Kind.TOP, null, null); }
        static Lattice constant(Value v) { return new Lattice(Kind.CONST, v, null); }
        static Lattice copy(Variable v) { return new Lattice(Kind.COPY, null, v); }
    }

    public boolean run(ControlFlowGraph cfg) {
        Map<BasicBlock, Map<Variable, Lattice>> in = new HashMap<>();
        Map<BasicBlock, Map<Variable, Lattice>> out = new HashMap<>();
        Set<BasicBlock> worklist = new HashSet<>(cfg.getBlocks());
        boolean changed = false;

        while (!worklist.isEmpty()) {
            BasicBlock block = worklist.iterator().next();
            worklist.remove(block);
            if (block.isUnreachable()) continue;
            Map<Variable, Lattice> inState = meetPredecessors(block, out);
            Map<Variable, Lattice> outState = transfer(block, new HashMap<>(inState));
            if (!out.containsKey(block) || !out.get(block).equals(outState)) {
                out.put(block, outState);
                worklist.addAll(block.getSuccessors());
            }
            in.put(block, inState);
        }

        for (BasicBlock block : cfg.getBlocks()) {
            if (block.isUnreachable()) continue;
            Map<Variable, Lattice> state = in.getOrDefault(block, new HashMap<>());
            changed |= rewriteBlock(block, state);
        }

        return changed;
    }

    private Map<Variable, Lattice> meetPredecessors(BasicBlock block, Map<BasicBlock, Map<Variable, Lattice>> out) {
        Map<Variable, Lattice> result = new HashMap<>();
        if (block.getPredecessors().isEmpty()) {
            return result;
        }
        boolean first = true;
        for (BasicBlock pred : block.getPredecessors()) {
            Map<Variable, Lattice> predOut = out.get(pred);
            if (predOut == null) continue;
            if (first) {
                result.putAll(predOut);
                first = false;
            } else {
                Set<Variable> keys = new HashSet<>(result.keySet());
                keys.addAll(predOut.keySet());
                for (Variable v : keys) {
                    Lattice merged = meet(result.get(v), predOut.getOrDefault(v, Lattice.undef()));
                    result.put(v, merged);
                }
            }
        }
        return result;
    }

    private Lattice meet(Lattice a, Lattice b) {
        if (a == null) return b == null ? Lattice.undef() : b;
        if (b == null) return a;
        if (a.kind == Kind.UNDEF) return b;
        if (b.kind == Kind.UNDEF) return a;
        if (a.kind == Kind.TOP || b.kind == Kind.TOP) return Lattice.top();
        if (a.kind == Kind.CONST && b.kind == Kind.CONST && Objects.equals(a.constVal, b.constVal)) {
            return a;
        }
        if (a.kind == Kind.COPY && b.kind == Kind.COPY && Objects.equals(a.copyOf, b.copyOf)) {
            return a;
        }
        return Lattice.top();
    }

    private Map<Variable, Lattice> transfer(BasicBlock block, Map<Variable, Lattice> state) {
        for (TAC tac : block.getInstructions()) {
            if (tac instanceof ir.tac.Call call) {
                if (call.hasDestination()) {
                    state.put(call.destination(), Lattice.top());
                }
                continue;
            }
            if (tac instanceof Assign assign) {
                Value left = substitute(assign.leftOperand(), state);
                Value right = substitute(assign.rightOperand(), state);
                Lattice newVal = eval(assign, left, right);
                state.put(assign.destination(), newVal);
            }
        }
        return state;
    }

    private boolean rewriteBlock(BasicBlock block, Map<Variable, Lattice> inState) {
        boolean changed = false;
        Map<Variable, Lattice> state = new HashMap<>(inState);
        for (int i = 0; i < block.getInstructions().size(); i++) {
            TAC tac = block.getInstructions().get(i);
            if (tac instanceof Assign assign) {
                Value l = substitute(assign.leftOperand(), state);
                Value r = substitute(assign.rightOperand(), state);
                TAC rewritten = rewriteAssign(assign, l, r);
                if (rewritten != tac) {
                    block.replaceInstruction(i, rewritten);
                    changed = true;
                }
                Lattice newVal = eval(assign, l, r);
                state.put(assign.destination(), newVal);
            } else if (tac instanceof ir.tac.Call call) {
                ir.tac.ValueList args = rewriteArgs(call.arguments(), state);
                if (!args.asList().equals(call.arguments().asList())) {
                    if (call.hasDestination()) {
                        block.replaceInstruction(i, new ir.tac.Call(call.id(), call.destination(), call.function(), args));
                    } else {
                        block.replaceInstruction(i, new ir.tac.Call(call.id(), call.function(), args));
                    }
                    changed = true;
                }
                if (call.hasDestination()) {
                    state.put(call.destination(), Lattice.top());
                }
            } else if (tac instanceof ConditionalBranch cb) {
                if (isLoopHeader(block)) {
                    // still substitute operands but do not fold branch to preserve loop structure
                    Value l = substitute(cb.left(), state);
                    Value r = substitute(cb.right(), state);
                    if (!l.equals(cb.left()) || (r != null && !r.equals(cb.right()))) {
                        block.replaceInstruction(i, new ConditionalBranch(cb.id(), cb.condition(), l, r, cb.trueTarget(), cb.falseTarget()));
                        changed = true;
                    }
                    continue;
                }
                Value l = substitute(cb.left(), state);
                Value r = substitute(cb.right(), state);
                Boolean result = evalCondition(cb.condition(), l, r);
                if (result != null) {
                    BasicBlock target = result ? cb.trueTarget() : cb.falseTarget();
                    block.replaceInstruction(i, new Branch(cb.id(), target));
                    block.clearSuccessors();
                    block.addSuccessor(target);
                    BasicBlock unreachable = result ? cb.falseTarget() : cb.trueTarget();
                    if (unreachable != null) unreachable.setUnreachable(true);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private ir.tac.ValueList rewriteArgs(ir.tac.ValueList args, Map<Variable, Lattice> state) {
        ir.tac.ValueList out = new ir.tac.ValueList();
        for (Value v : args) {
            out.add(substitute(v, state));
        }
        return out;
    }

    private Value substitute(Value v, Map<Variable, Lattice> state) {
        if (!(v instanceof Variable var)) return v;
        Lattice l = state.get(var);
        if (l == null) return v;
        if (l.kind == Kind.CONST) {
            return l.constVal;
        }
        if (l.kind == Kind.COPY && l.copyOf != null && !l.copyOf.equals(var)) {
            return substitute(l.copyOf, state);
        }
        return v;
    }

    private Lattice eval(Assign assign, Value left, Value right) {
        if (assign instanceof Copy) {
            if (left instanceof Literal) {
                return Lattice.constant(left);
            } else if (left instanceof Variable var) {
                return Lattice.copy(var);
            }
            return Lattice.top();
        }
        Integer l = ValueUtils.asInt(left);
        Integer r = ValueUtils.asInt(right);
        Float lf = ValueUtils.asFloat(left);
        Float rf = ValueUtils.asFloat(right);
        if (l != null && r != null) {
            int res;
            if (assign instanceof Add) res = l + r;
            else if (assign instanceof Sub) res = l - r;
            else if (assign instanceof Mul) res = l * r;
            else if (assign instanceof Div && r != 0) res = l / r;
            else return Lattice.top();
            return Lattice.constant(ValueUtils.intLiteral(res));
        }
        if (lf != null && rf != null) {
            float res;
            if (assign instanceof Add) res = lf + rf;
            else if (assign instanceof Sub) res = lf - rf;
            else if (assign instanceof Mul) res = lf * rf;
            else if (assign instanceof Div && rf != 0) res = lf / rf;
            else return Lattice.top();
            return Lattice.constant(ValueUtils.floatLiteral(res));
        }
        return Lattice.top();
    }

    private TAC rewriteAssign(Assign assign, Value left, Value right) {
        Integer l = ValueUtils.asInt(left);
        Integer r = ValueUtils.asInt(right);
        Float lf = ValueUtils.asFloat(left);
        Float rf = ValueUtils.asFloat(right);
        if (assign instanceof Copy) {
            return new Copy(assign.id(), assign.destination(), left);
        }
        if (l != null && r != null) {
            int res;
            if (assign instanceof Add) res = l + r;
            else if (assign instanceof Sub) res = l - r;
            else if (assign instanceof Mul) res = l * r;
            else if (assign instanceof Div && r != 0) res = l / r;
            else return assign;
            return new Copy(assign.id(), assign.destination(), ValueUtils.intLiteral(res));
        }
        if (lf != null && rf != null) {
            float res;
            if (assign instanceof Add) res = lf + rf;
            else if (assign instanceof Sub) res = lf - rf;
            else if (assign instanceof Mul) res = lf * rf;
            else if (assign instanceof Div && rf != 0) res = lf / rf;
            else return assign;
            return new Copy(assign.id(), assign.destination(), ValueUtils.floatLiteral(res));
        }
        if (assign instanceof Add) {
            if (r != null && r == 0) return new Copy(assign.id(), assign.destination(), left);
            if (l != null && l == 0) return new Copy(assign.id(), assign.destination(), right);
            if (rf != null && rf == 0.0f) return new Copy(assign.id(), assign.destination(), left);
            if (lf != null && lf == 0.0f) return new Copy(assign.id(), assign.destination(), right);
            if (!left.equals(assign.leftOperand()) || !Objects.equals(right, assign.rightOperand())) {
                return new Add(assign.id(), assign.destination(), left, right);
            }
        }
        if (assign instanceof Sub) {
            if (r != null && r == 0) return new Copy(assign.id(), assign.destination(), left);
            if (rf != null && rf == 0.0f) return new Copy(assign.id(), assign.destination(), left);
            if (!left.equals(assign.leftOperand()) || !Objects.equals(right, assign.rightOperand())) {
                return new Sub(assign.id(), assign.destination(), left, right);
            }
        }
        if (assign instanceof Mul) {
            if (r != null && r == 1) return new Copy(assign.id(), assign.destination(), left);
            if (l != null && l == 1) return new Copy(assign.id(), assign.destination(), right);
            if ((l != null && l == 0) || (r != null && r == 0)) {
                return new Copy(assign.id(), assign.destination(), ValueUtils.intLiteral(0));
            }
            if (rf != null && rf == 1.0f) return new Copy(assign.id(), assign.destination(), left);
            if (lf != null && lf == 1.0f) return new Copy(assign.id(), assign.destination(), right);
            if ((lf != null && lf == 0.0f) || (rf != null && rf == 0.0f)) {
                return new Copy(assign.id(), assign.destination(), ValueUtils.floatLiteral(0.0f));
            }
            if (!left.equals(assign.leftOperand()) || !Objects.equals(right, assign.rightOperand())) {
                return new Mul(assign.id(), assign.destination(), left, right);
            }
        }
        if (assign instanceof Div) {
            if (r != null && r == 1) return new Copy(assign.id(), assign.destination(), left);
            if (rf != null && rf == 1.0f) return new Copy(assign.id(), assign.destination(), left);
            if (!left.equals(assign.leftOperand()) || !Objects.equals(right, assign.rightOperand())) {
                return new Div(assign.id(), assign.destination(), left, right);
            }
        }
        return assign;
    }

    private Boolean evalCondition(ConditionalBranch.Condition cond, Value left, Value right) {
        Integer l = ValueUtils.asInt(left);
        Integer r = ValueUtils.asInt(right);
        Float lf = ValueUtils.asFloat(left);
        Float rf = ValueUtils.asFloat(right);
        Boolean lb = ValueUtils.asBool(left);
        Boolean rb = ValueUtils.asBool(right);
        // identical operands
        if (left != null && right != null && left.equals(right)) {
            return switch (cond) {
                case EQUAL -> true;
                case NOT_EQUAL -> false;
                case GREATER, GREATER_EQUAL, LESS, LESS_EQUAL -> false;
                case ZERO -> l != null ? l == 0 : lb != null ? !lb : false;
                case NON_ZERO -> l != null ? l != 0 : lb != null ? lb : true;
            };
        }
        if (l == null && r == null && lf == null && rf == null && lb == null && rb == null) return null;
        int lv = l != null ? l : (lf != null ? lf.intValue() : (lb != null ? (lb ? 1 : 0) : 0));
        int rv = r != null ? r : (rf != null ? rf.intValue() : (rb != null ? (rb ? 1 : 0) : 0));
        return switch (cond) {
            case ZERO -> lv == 0;
            case NON_ZERO -> lv != 0;
            case GREATER -> lv > rv;
            case GREATER_EQUAL -> lv >= rv;
            case LESS -> lv < rv;
            case LESS_EQUAL -> lv <= rv;
            case EQUAL -> lv == rv;
            case NOT_EQUAL -> lv != rv;
        };
    }

    private boolean isLoopHeader(BasicBlock block) {
        // heuristic: header if any successor is also a predecessor (back-edge)
        for (BasicBlock succ : block.getSuccessors()) {
            if (block.getPredecessors().contains(succ)) {
                return true;
            }
        }
        return false;
    }
}
