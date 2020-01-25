package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expression;

public interface FieldHolder<T> {
    T field(String name, Expression expression);
}
