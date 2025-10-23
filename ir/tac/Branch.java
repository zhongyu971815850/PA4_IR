package ir.tac;

import java.util.Objects;

import ir.cfg.BasicBlock;

public class Branch extends TAC {

    private final BasicBlock target;

    public Branch(int id, BasicBlock target) {
        super(id);
        this.target = Objects.requireNonNull(target, "Branch target cannot be null");
    }

    public BasicBlock target() {
        return target;
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "br " + target.getLabel();
    }
}
