package dev.morphia.aggregation.experimental.stages;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;

public class Group extends Stage {
    private List<Expression> id;
    private final List<Expression> fields = new ArrayList<>();

    protected Group(final List<Expression> values) {
        super("$group");
        id = values;
    }

    public List<Expression> getId() {
        return id;
    }

    public Group fields(Expression expression) {
        fields.add(expression);
        return this;
    }

    public List<Expression> getFields() {
        return fields;
    }

    public static Group id(final String name) {
        return new Group(List.of(Expression.field(name)));
    }

    public static Group nullId() {
        return new Group(null);
    }

    public static Group id(Expression id, Expression... values) {
        List<Expression> list = new ArrayList<>(of(id));
        list.addAll(of(values));
        return new Group(list);
    }
}
