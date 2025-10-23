package ir.tac;

import java.util.Objects;

import ast.BoolLiteral;
import ast.Expression;
import ast.FloatLiteral;
import ast.IntegerLiteral;

public class Literal implements Value {

    private final Expression value;

    public Literal(Expression value) {
        this.value = Objects.requireNonNull(value, "Literal expression cannot be null");
    }

    public Expression value() {
        return value;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        if (value instanceof BoolLiteral) {
            return String.valueOf(((BoolLiteral) value).value());
        }
        if (value instanceof IntegerLiteral) {
            return String.valueOf(((IntegerLiteral) value).value());
        }
        if (value instanceof FloatLiteral) {
            return String.valueOf(((FloatLiteral) value).value());
        }
        return value.toString();
    }
}
