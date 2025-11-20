package ir.tac;

import java.util.Objects;

import mocha.Symbol;

public class Variable implements Value {

    private final Symbol symbol;
    private int version; // SSA version, -1 when not in SSA form

    public Variable(Symbol symbol) {
        this(symbol, -1);
    }

    public Variable(Symbol symbol, int version) {
        this.symbol = Objects.requireNonNull(symbol, "Variable symbol cannot be null");
        this.version = version;
    }

    public Symbol symbol() {
        return symbol;
    }

    public int version() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Variable other)) return false;
        return Objects.equals(symbol, other.symbol) && version == other.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, version);
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        if (version >= 0) {
            return symbol.name() + "_" + version;
        }
        return symbol.name();
    }
}
