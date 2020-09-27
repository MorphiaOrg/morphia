package dev.morphia.aggregation.experimental.expressions.impls;

public interface FieldHolder<T> {
    T field(String name, Expression expression);
}
