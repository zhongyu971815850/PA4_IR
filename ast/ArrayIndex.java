package ast;

public class ArrayIndex extends Node implements Expression {
    private Expression array;
    private Expression index;

    public ArrayIndex(int lineNum, int charPos, Expression array, Expression index) {
        super(lineNum, charPos);
        this.array = array;
        this.index = index;
    }

    public Expression array() {
        return array;
    }

    public Expression index() {
        return index;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
