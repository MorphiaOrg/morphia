package dev.morphia.aggregation.experimental.expressions.internal;

import dev.morphia.aggregation.experimental.expressions.Expression;

public interface FieldHolder<T> {
    T field(String name, Expression expression);
}
