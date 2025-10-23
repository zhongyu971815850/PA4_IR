package ir.tac;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import ir.cfg.BasicBlock;

public class Phi extends TAC {

    public static class Incoming {
        private final BasicBlock block;
        private final Value value;

        public Incoming(BasicBlock block, Value value) {
            this.block = Objects.requireNonNull(block, "Phi incoming block cannot be null");
            this.value = Objects.requireNonNull(value, "Phi incoming value cannot be null");
        }

        public BasicBlock block() {
            return block;
        }

        public Value value() {
            return value;
        }

        @Override
        public String toString() {
            return block.getLabel() + ": " + value;
        }
    }

    private final Variable destination;
    private final Map<BasicBlock, Value> incoming;

    public Phi(int id, Variable destination) {
        super(id);
        this.destination = Objects.requireNonNull(destination, "Phi destination cannot be null");
        this.incoming = new LinkedHashMap<>();
    }

    public Variable destination() {
        return destination;
    }

    public Map<BasicBlock, Value> incoming() {
        return Collections.unmodifiableMap(incoming);
    }

    public void addIncoming(BasicBlock block, Value value) {
        incoming.put(Objects.requireNonNull(block, "incoming block"),
                Objects.requireNonNull(value, "incoming value"));
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        destination.accept(visitor);
        for (Value value : incoming.values()) {
            value.accept(visitor);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(destination()).append(" = phi (");
        boolean first = true;
        for (Map.Entry<BasicBlock, Value> entry : incoming.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey().getLabel()).append(": ")
                    .append(entry.getValue());
            first = false;
        }
        builder.append(')');
        return builder.toString();
    }
}
