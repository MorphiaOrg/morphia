package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayLiteral extends ArrayExpression {

    private Object[] objects;

    public ArrayLiteral(Expression... values) {
        super("unused", asList(values));
    }

    public ArrayLiteral(Object... objects) {
        super("unused", emptyList());
        this.objects = objects;
    }

    @Nullable
    public Object[] objects() {
        return objects;
    }
}
