package ast;

import mocha.Symbol;

public class Designator extends Node implements Expression {
    private Symbol symbol;

    public Designator(int lineNum, int charPos, Symbol symbol) {
        super(lineNum, charPos);
        this.symbol = symbol;
    }

    public Symbol symbol() {
        return symbol;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
