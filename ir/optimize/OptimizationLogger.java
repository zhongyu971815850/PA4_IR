package ir.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Objects;

import ir.cfg.BasicBlock;
import ir.tac.TAC;

/**
 * Lightweight logging utility to trace optimization passes.
 */
public final class OptimizationLogger {

    private static boolean enabled = false;
    private static PrintStream sink = System.out;

    private OptimizationLogger() {
    }

    public static void enable() {
        enabled = true;
        sink = System.out;
    }

    public static void enable(PrintStream stream) {
        enabled = true;
        sink = Objects.requireNonNull(stream, "Log stream cannot be null");
    }

    public static void enableToFile(String path) throws FileNotFoundException {
        enable(new PrintStream(new FileOutputStream(path)));
    }

    public static void disable() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String pass, String function, String message) {
        if (!enabled) {
            return;
        }
        sink.printf("[%s] %s: %s%n", pass, function, message);
    }

    public static void log(String pass, String function, BasicBlock block, TAC tac, String message) {
        if (!enabled) {
            return;
        }
        sink.printf("[%s] %s %s %s: %s%n",
                pass,
                function,
                block != null ? block.getLabel() : "<no-block>",
                tac != null ? tac.toString() : "<no-instr>",
                message);
    }
}
