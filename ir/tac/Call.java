package ir.tac;

import java.util.Objects;

import mocha.Symbol;

public class Call extends TAC {

    private final Symbol function;
    private final ValueList arguments;
    private final Variable destination;

    public Call(int id, Symbol function, ValueList arguments) {
        this(id, null, function, arguments);
    }

    public Call(int id, Variable destination, Symbol function, ValueList arguments) {
        super(id);
        this.destination = destination;
        this.function = Objects.requireNonNull(function, "Call target function cannot be null");
        this.arguments = arguments != null ? arguments : new ValueList();
    }

    public Symbol function() {
        return function;
    }

    public ValueList arguments() {
        return arguments;
    }

    public Variable destination() {
        return destination;
    }

    public boolean hasDestination() {
        return destination != null;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        arguments.accept(visitor);
        if (destination != null) {
            destination.accept(visitor);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (destination != null) {
            builder.append(destination).append(" = ");
        }
        builder.append("call ").append(function.name()).append('(').append(arguments).append(')');
        return builder.toString();
    }
}
