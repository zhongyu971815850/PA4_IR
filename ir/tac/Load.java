package ir.tac;

public class Load extends Assign {

    public Load(int id, Variable dest, Value address) {
        super(id, dest, address, null);
    }

    @Override
    protected String opcode() {
        return "load";
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        visitChildren(visitor);
    }
}
