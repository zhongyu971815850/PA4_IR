package types;

public class FuncType extends Type {

    private TypeList params;
    private Type returnType;

    public FuncType(TypeList params, Type returnType) {
        this.params = params;
        this.returnType = returnType;
    }
    
    public TypeList params() {
        return params;
    }
    
    public Type returnType() {
        return returnType;
    }
    
    @Override
    public Type call(Type args) {
        if (args instanceof TypeList) {
            TypeList argList = (TypeList) args;
            if (argList.getList().size() == params.getList().size()) {
                boolean match = true;
                for (int i = 0; i < params.getList().size(); i++) {
                    if (!argList.getList().get(i).getClass().equals(params.getList().get(i).getClass())) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return returnType;
                }
            }
        }
        return super.call(args);
    }
    
    @Override
    public String toString() {
        return "(" + params + ")->" + returnType;
    }
}
