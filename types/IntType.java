package types;

public class IntType extends Type {

    @Override
    public Type add(Type that) {
        if (that instanceof IntType) {
            return new IntType();
        }
        return super.add(that);
    }

    @Override
    public Type sub(Type that) {
        if (that instanceof IntType) {
            return new IntType();
        }
        return super.sub(that);
    }

    @Override
    public Type mul(Type that) {
        if (that instanceof IntType) {
            return new IntType();
        }
        return super.mul(that);
    }

    @Override
    public Type div(Type that) {
        if (that instanceof IntType) {
            return new IntType();
        }
        return super.div(that);
    }

    @Override
    public Type mod(Type that) {
        if (that instanceof IntType) {
            return new IntType();
        }
        return super.mod(that);
    }

    @Override
    public Type compare(Type that) {
        if (that instanceof IntType) {
            return new BoolType();
        }
        return super.compare(that);
    }

    @Override
    public String toString() {
        return "int";
    }
}
