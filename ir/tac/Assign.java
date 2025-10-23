package ir.tac;

import java.util.Objects;

public abstract class Assign extends TAC {

    private final Variable dest; // lhs
    private final Value left; // operand_1
    private final Value right; // operand_2

    protected Assign(int id, Variable dest, Value left, Value right) {
        super(id);
        this.dest = Objects.requireNonNull(dest, "Destination variable cannot be null");
        this.left = left;
        this.right = right;
    }

    public Variable destination() {
        return dest;
    }

    public Value leftOperand() {
        return left;
    }

    public Value rightOperand() {
        return right;
    }

    protected abstract String opcode();

    protected void visitChildren(TACVisitor visitor) {
        destination().accept(visitor);
        if (left != null) {
            left.accept(visitor);
        }
        if (right != null) {
            right.accept(visitor);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(destination()).append(" = ").append(opcode());
        if (left != null) {
            builder.append(' ').append(left);
        }
        if (right != null) {
            builder.append(", ").append(right);
        }
        return builder.toString();
    }
}
