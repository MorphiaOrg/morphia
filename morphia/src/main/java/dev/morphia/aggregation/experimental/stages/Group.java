package dev.morphia.aggregation.experimental.stages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.List.of;

public class Group implements Stage {
    private String name;
    private List<Expression> compoundId;
    private final List<Expression> expressions = new ArrayList<>();

    protected Group(final String name) {
        this.name = name;
    }

    protected Group(final List<Expression> values) {
        compoundId = values;
    }

    public Group expressions(Expression expression) {
        expressions.add(expression);
        return this;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public static Group id(final String name) {
        return new Group(name.startsWith("$") ? name : "$" + name);
    }

    public static Group id(Expression id, Expression... values) {
        List<Expression> list = new ArrayList<>(of(id));
        list.addAll(of(values));
        return new Group(list);
    }

    public String getName() {
        return name;
    }

    public List<Expression> getCompoundId() {
        return compoundId;
    }
}
