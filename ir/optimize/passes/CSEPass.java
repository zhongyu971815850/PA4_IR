package ir.optimize.passes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ir.cfg.BasicBlock;
import ir.tac.Add;
import ir.tac.Assign;
import ir.tac.Div;
import ir.tac.Mul;
import ir.tac.Sub;
import ir.tac.TAC;
import ir.tac.Value;
import ir.tac.Variable;

/**
 * Local common subexpression elimination (per basic block).
 */
public class CSEPass {

    private static final class ExprKey {
        private final Class<?> op;
        private final Value a;
        private final Value b;

        ExprKey(Class<?> op, Value a, Value b) {
            // normalize commutative ops (add, mul) by ordering operands
            if (isCommutative(op) && compare(b, a) < 0) {
                this.op = op;
                this.a = b;
                this.b = a;
            } else {
                this.op = op;
                this.a = a;
                this.b = b;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExprKey k)) return false;
            return op.equals(k.op) && Objects.equals(a, k.a) && Objects.equals(b, k.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, a, b);
        }

        private static boolean isCommutative(Class<?> op) {
            return op == Add.class || op == Mul.class;
        }

        private static int compare(Value v1, Value v2) {
            // cheap total order via toString; relies on equals/hashcode to match
            return v1.toString().compareTo(v2.toString());
        }
    }

    public boolean run(BasicBlock block) {
        if (block.isUnreachable()) {
            return false;
        }
        Map<ExprKey, Variable> table = new HashMap<>();
        boolean changed = false;
        for (int i = 0; i < block.getInstructions().size(); i++) {
            TAC tac = block.getInstructions().get(i);
            if (!(tac instanceof Assign assign)) {
                continue;
            }
            Value left = assign.leftOperand();
            Value right = assign.rightOperand();

            if (isExpression(assign)) {
                ExprKey key = new ExprKey(assign.getClass(), left, right);
                Variable existing = table.get(key);
                if (existing != null) {
                    // replace with copy from existing result
                    block.replaceInstruction(i, new ir.tac.Copy(assign.id(), assign.destination(), existing));
                    changed = true;
                } else {
                    table.put(key, assign.destination());
                }
            }

            // kill expressions that referenced redefined vars
            if (assign.destination() != null) {
                Variable def = assign.destination();
                table.entrySet().removeIf(e -> usesVar(e.getKey(), def));
            }
        }
        return changed;
    }

    private boolean isExpression(Assign assign) {
        return assign instanceof Add || assign instanceof Sub || assign instanceof Mul || assign instanceof Div;
    }

    private boolean usesVar(ExprKey key, Variable var) {
        return (key.a instanceof Variable v1 && v1.equals(var))
                || (key.b instanceof Variable v2 && v2.equals(var));
    }
}
