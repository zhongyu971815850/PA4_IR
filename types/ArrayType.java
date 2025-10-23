package types;

public class ArrayType extends Type {

    private Type baseType;
    private int size;

    public ArrayType(Type baseType, int size) {
        this.baseType = baseType;
        this.size = size;
    }
    
    public Type baseType() {
        return baseType;
    }
    
    public int size() {
        return size;
    }
    
    @Override
    public Type index(Type that) {
        if (that instanceof IntType) {
            return baseType;
        }
        return super.index(that);
    }
    
    @Override
    public String toString() {
        return baseType.toString() + "[" + size + "]";
    }
}
