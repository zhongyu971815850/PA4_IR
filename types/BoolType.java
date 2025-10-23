package types;

public class BoolType extends Type {

    @Override
    public Type and(Type that) {
        if (that instanceof BoolType) {
            return new BoolType();
        }
        return super.and(that);
    }

    @Override
    public Type or(Type that) {
        if (that instanceof BoolType) {
            return new BoolType();
        }
        return super.or(that);
    }

    @Override
    public Type not() {
        return new BoolType();
    }

    @Override
    public String toString() {
        return "bool";
    }
}
