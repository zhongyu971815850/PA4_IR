package ir.cfg;

import java.util.Iterator;

import ir.tac.TAC;

public class CFGPrinter implements CFGVisitor {

    private final StringBuilder builder = new StringBuilder();

    public String print(ControlFlowGraph graph) {
        builder.setLength(0);
        builder.append("digraph CFG {\n");
        builder.append("  node [shape=record];\n");
        graph.accept(this);
        builder.append("}\n");
        return builder.toString();
    }

    @Override
    public void visit(ControlFlowGraph graph) {
        for (BasicBlock block : graph.getBlocks()) {
            block.accept(this);
        }
        for (BasicBlock block : graph.getBlocks()) {
            for (BasicBlock successor : block.getSuccessors()) {
                builder.append("  ")
                        .append(block.getLabel())
                        .append(" -> ")
                        .append(successor.getLabel())
                        .append(";\n");
            }
            BasicBlock idom = block.getImmediateDominator();
            if (idom != null) {
                builder.append("  ")
                        .append(idom.getLabel())
                        .append(" -> ")
                        .append(block.getLabel())
                        .append(" [style=dashed,label=\"idom\"];\n");
            }
        }
    }

    @Override
    public void visit(BasicBlock block) {
        builder.append("  ")
                .append(block.getLabel())
                .append(" [label=\"")
                .append(block.getLabel())
                .append(":\\l");
        Iterator<TAC> iterator = block.iterator();
        while (iterator.hasNext()) {
            TAC tac = iterator.next();
            if (tac.isEliminated()) {
                continue;
            }
            builder.append(escape(tac.toString())).append("\\l");
        }
        if (!block.getDominators().isEmpty()) {
            builder.append("|dom={");
            boolean first = true;
            for (BasicBlock dom : block.getDominators()) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(dom.getLabel());
                first = false;
            }
            builder.append("}");
        }
        builder.append("\"];\n");
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
