package ir.tac;

public interface TACVisitor {
    void visit(Add add);
    void visit(Sub sub);
    void visit(Mul mul);
    void visit(Div div);
    void visit(Pow pow);
    void visit(Neg neg);
    void visit(Copy copy);
    void visit(Load load);
    void visit(Store store);
    void visit(Phi phi);
    void visit(Call call);
    void visit(Return ret);
    void visit(Read read);
    void visit(Write write);
    void visit(Branch branch);
    void visit(ConditionalBranch branch);
    void visit(Literal literal);
    void visit(Variable variable);
    void visit(ValueList valueList);
}
