package dev.morphia.aggregation.experimental.expressions;

public interface FieldHolder<T> {
    T field(String name, Expression expression);
}
