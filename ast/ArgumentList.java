package ast;

import java.util.ArrayList;
import java.util.List;

public class ArgumentList extends Node {
    private List<Expression> arguments;

    public ArgumentList(int lineNum, int charPos) {
        super(lineNum, charPos);
        this.arguments = new ArrayList<>();
    }

    public void addArgument(Expression argument) {
        arguments.add(argument);
    }

    public List<Expression> arguments() {
        return arguments;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
