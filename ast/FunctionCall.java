package ast;

import mocha.Symbol;

public class FunctionCall extends Node implements Expression, Statement {
    private Symbol function;
    private ArgumentList arguments;

    public FunctionCall(int lineNum, int charPos, Symbol function, ArgumentList arguments) {
        super(lineNum, charPos);
        this.function = function;
        this.arguments = arguments;
    }

    public Symbol function() {
        return function;
    }

    public ArgumentList arguments() {
        return arguments;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
