package ir.tac;

import java.util.Objects;

public class Read extends TAC {

    private final Variable destination;

    public Read(int id, Variable destination) {
        super(id);
        this.destination = Objects.requireNonNull(destination, "Read destination cannot be null");
    }

    public Variable destination() {
        return destination;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        destination.accept(visitor);
    }

    @Override
    public String toString() {
        return "read " + destination;
    }
}
