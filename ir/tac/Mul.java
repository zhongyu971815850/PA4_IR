package ir.tac;

public class Mul extends Assign {

    public Mul(int id, Variable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    protected String opcode() {
        return "mul";
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        visitChildren(visitor);
    }
}
