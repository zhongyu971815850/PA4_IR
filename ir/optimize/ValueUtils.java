package ir.optimize;

import ast.BoolLiteral;
import ast.FloatLiteral;
import ast.IntegerLiteral;
import ir.tac.Literal;
import ir.tac.Value;

public final class ValueUtils {
    private ValueUtils() {}

    public static boolean isLiteral(Value v) {
        return v instanceof Literal;
    }

    public static boolean isIntLiteral(Value v) {
        return v instanceof Literal lit && lit.value() instanceof IntegerLiteral;
    }

    public static boolean isFloatLiteral(Value v) {
        return v instanceof Literal lit && lit.value() instanceof FloatLiteral;
    }

    public static Integer asInt(Value v) {
        if (v instanceof Literal lit && lit.value() instanceof IntegerLiteral intLit) {
            return intLit.value();
        }
        return null;
    }

    public static Float asFloat(Value v) {
        if (v instanceof Literal lit && lit.value() instanceof FloatLiteral f) {
            return (float) f.value();
        }
        return null;
    }

    public static Literal intLiteral(int v) {
        return new Literal(new IntegerLiteral(0, 0, new mocha.Token(String.valueOf(v), 0, 0)));
    }

    public static Literal boolLiteral(boolean v) {
        return new Literal(new BoolLiteral(0, 0, new mocha.Token(v ? "true" : "false", 0, 0)));
    }

    public static Boolean asBool(Value v) {
        if (v instanceof Literal lit && lit.value() instanceof BoolLiteral b) {
            return b.value();
        }
        return null;
    }

    public static Literal floatLiteral(float f) {
        return new Literal(new FloatLiteral(0, 0, new mocha.Token(Float.toString(f), 0, 0)));
    }
}
