package ir.tac;

import java.util.Objects;

public class Store extends TAC {

    private final Value address;
    private final Value value;

    public Store(int id, Value address, Value value) {
        super(id);
        this.address = Objects.requireNonNull(address, "Store address cannot be null");
        this.value = Objects.requireNonNull(value, "Store value cannot be null");
    }

    public Value address() {
        return address;
    }

    public Value value() {
        return value;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        address.accept(visitor);
        value.accept(visitor);
    }

    @Override
    public String toString() {
        return "store " + address + ", " + value;
    }
}
