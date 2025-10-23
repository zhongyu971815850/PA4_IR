package types;

public class FloatType extends Type {

    @Override
    public Type add(Type that) {
        if (that instanceof FloatType) {
            return new FloatType();
        }
        return super.add(that);
    }

    @Override
    public Type sub(Type that) {
        if (that instanceof FloatType) {
            return new FloatType();
        }
        return super.sub(that);
    }

    @Override
    public Type mul(Type that) {
        if (that instanceof FloatType) {
            return new FloatType();
        }
        return super.mul(that);
    }

    @Override
    public Type div(Type that) {
        if (that instanceof FloatType) {
            return new FloatType();
        }
        return super.div(that);
    }

    @Override
    public Type compare(Type that) {
        if (that instanceof FloatType) {
            return new BoolType();
        }
        return super.compare(that);
    }

    @Override
    public String toString() {
        return "float";
    }
}
