package ir.tac;

public class Div extends Assign {

    public Div(int id, Variable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    protected String opcode() {
        return "div";
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        visitChildren(visitor);
    }
}
