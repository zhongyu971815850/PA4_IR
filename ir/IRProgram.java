package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

import ir.cfg.CFGPrinter;

/**
 * Container for all IR functions emitted by the frontend.
 */
public class IRProgram {

    private final List<IRFunction> functions = new ArrayList<>();
    private IRFunction entryFunction;
    private final Set<mocha.Symbol> globals = new HashSet<>();

    public void addFunction(IRFunction function) {
        Objects.requireNonNull(function, "IRFunction cannot be null");
        functions.add(function);
        if (entryFunction == null) {
            entryFunction = function;
        }
    }

    public List<IRFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    public IRFunction getEntryFunction() {
        return entryFunction;
    }

    public void setEntryFunction(IRFunction entryFunction) {
        this.entryFunction = Objects.requireNonNull(entryFunction, "Entry function cannot be null");
    }

    public void setGlobalSymbols(Set<mocha.Symbol> symbols) {
        globals.clear();
        if (symbols != null) {
            globals.addAll(symbols);
        }
    }

    public Set<mocha.Symbol> getGlobalSymbols() {
        return Collections.unmodifiableSet(globals);
    }

    /**
     * Emit a combined DOT string for every function CFG.
     */
    public String toDot() {
        StringBuilder builder = new StringBuilder();
        CFGPrinter printer = new CFGPrinter();
        for (IRFunction function : functions) {
            builder.append(printer.print(function.getControlFlowGraph(), function.getName()))
                    .append('\n');
        }
        return builder.toString();
    }
}
