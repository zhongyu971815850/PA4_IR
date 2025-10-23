package mocha;

import java.util.HashMap;
import java.util.Map;
import types.*;

public class SymbolTable {

    private Map<String, Symbol> symbols;

    public SymbolTable () {
        symbols = new HashMap<>();
        // Initialize predefined functions
        initializePredefinedFunctions();
    }
    
    private void initializePredefinedFunctions() {
        // Add all predefined functions
        try {
            // Read functions (no parameters)
            insert("readInt", new FuncType(new TypeList(), new IntType()), true);
            insert("readFloat", new FuncType(new TypeList(), new FloatType()), true);
            insert("readBool", new FuncType(new TypeList(), new BoolType()), true);
            
            // Print functions (single parameter)
            TypeList printIntParams = new TypeList();
            printIntParams.append(new IntType());
            insert("printInt", new FuncType(printIntParams, new VoidType()), true);
            
            TypeList printFloatParams = new TypeList();
            printFloatParams.append(new FloatType());
            insert("printFloat", new FuncType(printFloatParams, new VoidType()), true);
            
            TypeList printBoolParams = new TypeList();
            printBoolParams.append(new BoolType());
            insert("printBool", new FuncType(printBoolParams, new VoidType()), true);
            
            // Line printing function (no parameters)
            insert("println", new FuncType(new TypeList(), new VoidType()), true);
            
            // Array copy function (3 parameters: dest array, src array, count)
            TypeList arrcpyParams = new TypeList();
            arrcpyParams.append(new ArrayType(new IntType(), -1)); // dest array
            arrcpyParams.append(new ArrayType(new IntType(), -1)); // src array  
            arrcpyParams.append(new IntType()); // count
            insert("arrcpy", new FuncType(arrcpyParams, new VoidType()), true);
            
        } catch (RedeclarationError e) {
            // Should not happen during initialization
        }
    }

    // lookup name in SymbolTable
    public Symbol lookup (String name) throws SymbolNotFoundError {
        Symbol symbol = symbols.get(name);
        if (symbol == null) {
            throw new SymbolNotFoundError(name);
        }
        return symbol;
    }

    // insert name in SymbolTable
    public Symbol insert (String name, Type type, boolean isFunction) throws RedeclarationError {
        if (symbols.containsKey(name)) {
            throw new RedeclarationError(name);
        }
        Symbol symbol = new Symbol(name, type, isFunction);
        symbols.put(name, symbol);
        return symbol;
    }
    
    // insert name in SymbolTable (backward compatibility)
    public Symbol insert (String name) throws RedeclarationError {
        return insert(name, new ErrorType("Type not specified"), false);
    }
    
    public boolean contains(String name) {
        return symbols.containsKey(name);
    }
}

class SymbolNotFoundError extends Error {

    private static final long serialVersionUID = 1L;
    private final String name;

    public SymbolNotFoundError (String name) {
        super("Symbol " + name + " not found.");
        this.name = name;
    }

    public String name () {
        return name;
    }
}

class RedeclarationError extends Error {

    private static final long serialVersionUID = 1L;
    private final String name;

    public RedeclarationError (String name) {
        super("Symbol " + name + " being redeclared.");
        this.name = name;
    }

    public String name () {
        return name;
    }
}
