package ir.tac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ValueList implements Iterable<Value>, Visitable {

    private final List<Value> values;

    public ValueList() {
        this.values = new ArrayList<>();
    }

    public ValueList(List<? extends Value> initialValues) {
        this.values = new ArrayList<>(initialValues);
    }

    public void add(Value value) {
        values.add(value);
    }

    public List<Value> asList() {
        return Collections.unmodifiableList(values);
    }

    public int size() {
        return values.size();
    }

    @Override
    public Iterator<Value> iterator() {
        return values.iterator();
    }

    @Override
    public void accept(TACVisitor visitor) {
        visitor.visit(this);
        for (Value value : values) {
            value.accept(visitor);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Value value : values) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(value);
            first = false;
        }
        return builder.toString();
    }
}
