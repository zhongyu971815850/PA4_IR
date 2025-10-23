package types;

public class AddressOfType extends Type {
    private Type baseType;
    
    public AddressOfType(Type baseType) {
        this.baseType = baseType;
    }
    
    public Type baseType() {
        return baseType;
    }
    
    @Override
    public Type deref() {
        return baseType;
    }
    
    @Override
    public String toString() {
        return "AddressOf(" + baseType + ")";
    }
}
