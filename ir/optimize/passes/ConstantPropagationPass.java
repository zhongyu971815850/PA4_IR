package ir.optimize.passes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ir.cfg.BasicBlock;
import ir.optimize.ValueUtils;
import ir.tac.Add;
import ir.tac.Assign;
import ir.tac.Copy;
import ir.tac.Div;
import ir.tac.Literal;
import ir.tac.Mul;
import ir.tac.Sub;
import ir.tac.TAC;
import ir.tac.Value;
import ir.tac.Variable;

/**
 * Constant + copy propagation with constant folding for arithmetic.
 * Works per basic block (straight-line for now).
 */
public class ConstantPropagationPass {

    private enum Kind { UNDEF, CONST, TOP }

    private static final class Lattice {
        final Kind kind;
        final Value constVal; // only when kind == CONST

        private Lattice(Kind kind, Value constVal) {
            this.kind = kind;
            this.constVal = constVal;
        }

        static Lattice undef() { return new Lattice(Kind.UNDEF, null); }
        static Lattice top() { return new Lattice(Kind.TOP, null); }
        static Lattice constant(Value v) { return new Lattice(Kind.CONST, v); }
    }

    private final Map<Variable, Lattice> env = new HashMap<>();

    public boolean run(BasicBlock block) {
        env.clear();
        boolean changed = false;
        for (int i = 0; i < block.getInstructions().size(); i++) {
            TAC tac = block.getInstructions().get(i);
            if (tac instanceof Assign assign) {
                TAC rewritten = rewrite(assign);
                if (rewritten != tac) {
                    block.replaceInstruction(i, rewritten);
                    tac = rewritten;
                    changed = true;
                }
                updateEnv((Assign) tac);
            }
        }
        return changed;
    }

    private TAC rewrite(Assign assign) {
        Value left = substitute(assign.leftOperand());
        Value right = substitute(assign.rightOperand());

        // Copy propagation
        if (assign instanceof Copy) {
            return new Copy(assign.id(), assign.destination(), left);
        }

        // Constant folding for arithmetic
        Integer l = ValueUtils.asInt(left);
        Integer r = ValueUtils.asInt(right);
        if (l != null && r != null) {
            int result = 0;
            if (assign instanceof Add) {
                result = l + r;
            } else if (assign instanceof Sub) {
                result = l - r;
            } else if (assign instanceof Mul) {
                result = l * r;
            } else if (assign instanceof Div && r != 0) {
                result = l / r;
            } else {
                return assign; // unsupported op/div by zero
            }
            return new Copy(assign.id(), assign.destination(), ValueUtils.intLiteral(result));
        }

        // identity simplifications with literal 0/1
        if (assign instanceof Add) {
            if (r != null && r == 0) return new Copy(assign.id(), assign.destination(), left);
            if (l != null && l == 0) return new Copy(assign.id(), assign.destination(), right);
        }
        if (assign instanceof Sub) {
            if (r != null && r == 0) return new Copy(assign.id(), assign.destination(), left);
        }
        if (assign instanceof Mul) {
            if (r != null && r == 1) return new Copy(assign.id(), assign.destination(), left);
            if (l != null && l == 1) return new Copy(assign.id(), assign.destination(), right);
            if ((l != null && l == 0) || (r != null && r == 0)) {
                return new Copy(assign.id(), assign.destination(), ValueUtils.intLiteral(0));
            }
        }
        if (assign instanceof Div) {
            if (r != null && r == 1) return new Copy(assign.id(), assign.destination(), left);
        }

        // If operands changed, rebuild same op
        if (!Objects.equals(left, assign.leftOperand()) || !Objects.equals(right, assign.rightOperand())) {
            if (assign instanceof Add) {
                return new Add(assign.id(), assign.destination(), left, right);
            }
            if (assign instanceof Sub) {
                return new Sub(assign.id(), assign.destination(), left, right);
            }
            if (assign instanceof Mul) {
                return new Mul(assign.id(), assign.destination(), left, right);
            }
            if (assign instanceof Div) {
                return new Div(assign.id(), assign.destination(), left, right);
            }
        }
        return assign;
    }

    private Value substitute(Value v) {
        if (!(v instanceof Variable var)) {
            return v;
        }
        Lattice state = env.getOrDefault(var, Lattice.undef());
        if (state.kind == Kind.CONST) {
            return state.constVal;
        }
        return v;
    }

    private void updateEnv(Assign assign) {
        Variable dest = assign.destination();
        if (assign instanceof Copy copy) {
            Value src = copy.leftOperand();
            if (src instanceof Literal) {
                env.put(dest, Lattice.constant(src));
            } else if (src instanceof Variable var) {
                Lattice state = env.get(var);
                if (state != null && state.kind == Kind.CONST) {
                    env.put(dest, state);
                } else {
                    env.put(dest, Lattice.top());
                }
            } else {
                env.put(dest, Lattice.top());
            }
            return;
        }

        Integer l = ValueUtils.asInt(assign.leftOperand());
        Integer r = ValueUtils.asInt(assign.rightOperand());
        if (l != null && r != null) {
            int result = switch (assign) {
                case Add a -> l + r;
                case Sub s -> l - r;
                case Mul m -> l * r;
                case Div d -> (r != 0 ? l / r : l); // simplistic
                default -> l;
            };
            env.put(dest, Lattice.constant(ValueUtils.intLiteral(result)));
        } else {
            env.put(dest, Lattice.top());
        }
    }
}
