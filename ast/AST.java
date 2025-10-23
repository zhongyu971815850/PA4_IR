package ast;

public class AST {
    private Computation computation;

    public AST(Computation computation) {
        this.computation = computation;
    }

    public Computation computation() {
        return computation;
    }

    public String printPreOrder(){
        if (computation == null) {
            return "";
        }
        return computation.toString();
    }
}
