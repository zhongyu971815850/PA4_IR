package ir.tac;

public class Neg extends Assign {

    public Neg(int id, Variable dest, Value operand) {
        super(id, dest, operand, null);
    }

    @Override
    protected String opcode() {
        return "neg";
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        visitChildren(visitor);
    }
}
