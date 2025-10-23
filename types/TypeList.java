package types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeList extends Type implements Iterable<Type> {

    private List<Type> list;

    public TypeList () {
        list = new ArrayList<>();
    }

    public void append (Type type) {
        list.add(type);
    }

    public List<Type> getList () {
        return list;
    }

    @Override
    public Iterator<Type> iterator () {
        return list.iterator();
    }
    
    public int size() {
        return list.size();
    }
    
    public Type get(int index) {
        return list.get(index);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toString());
        }
        return sb.toString();
    }
}
