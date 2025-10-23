package ast;

import mocha.Symbol;
import types.Type;

public class FunctionDeclaration extends Node {
    private Symbol function;
    private FunctionBody body;
    private Type returnType;

    public FunctionDeclaration(int lineNum, int charPos, Symbol function, Type returnType, FunctionBody body) {
        super(lineNum, charPos);
        this.function = function;
        this.returnType = returnType;
        this.body = body;
    }

    public Symbol function() {
        return function;
    }

    public Type returnType() {
        return returnType;
    }

    public FunctionBody body() {
        return body;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
