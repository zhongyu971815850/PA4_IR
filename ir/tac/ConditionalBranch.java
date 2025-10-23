package ir.tac;

import java.util.Objects;

import ir.cfg.BasicBlock;

public class ConditionalBranch extends TAC {

    public enum Condition {
        ZERO("bz"),
        NON_ZERO("bnz"),
        GREATER("bg"),
        GREATER_EQUAL("bge"),
        LESS("blt"),
        LESS_EQUAL("ble"),
        EQUAL("be"),
        NOT_EQUAL("bne");

        private final String opcode;

        Condition(String opcode) {
            this.opcode = opcode;
        }

        public String opcode() {
            return opcode;
        }
    }

    private final Condition condition;
    private final Value left;
    private final Value right;
    private final BasicBlock trueTarget;
    private final BasicBlock falseTarget;

    public ConditionalBranch(int id, Condition condition, Value left, Value right,
            BasicBlock trueTarget, BasicBlock falseTarget) {
        super(id);
        this.condition = Objects.requireNonNull(condition, "Branch condition cannot be null");
        this.left = Objects.requireNonNull(left, "Branch left operand cannot be null");
        this.right = right;
        this.trueTarget = Objects.requireNonNull(trueTarget, "True target cannot be null");
        this.falseTarget = falseTarget;
    }

    public Condition condition() {
        return condition;
    }

    public Value left() {
        return left;
    }

    public Value right() {
        return right;
    }

    public BasicBlock trueTarget() {
        return trueTarget;
    }

    public BasicBlock falseTarget() {
        return falseTarget;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        left.accept(visitor);
        if (right != null) {
            right.accept(visitor);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(condition.opcode()).append(' ').append(left);
        if (right != null) {
            builder.append(", ").append(right);
        }
        builder.append(" -> ").append(trueTarget.getLabel());
        if (falseTarget != null) {
            builder.append(" else ").append(falseTarget.getLabel());
        }
        return builder.toString();
    }
}
