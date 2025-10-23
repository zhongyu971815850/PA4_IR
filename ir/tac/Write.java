package ir.tac;

import java.util.Objects;

public class Write extends TAC {

    private final Value value;

    public Write(int id, Value value) {
        super(id);
        this.value = Objects.requireNonNull(value, "Write value cannot be null");
    }

    public Value value() {
        return value;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        value.accept(visitor);
    }

    @Override
    public String toString() {
        return "write " + value;
    }
}
