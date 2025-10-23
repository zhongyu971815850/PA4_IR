package ir.tac;

public class Copy extends Assign {

    public Copy(int id, Variable dest, Value source) {
        super(id, dest, source, null);
    }

    @Override
    protected String opcode() {
        return "assign";
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        visitChildren(visitor);
    }
}
