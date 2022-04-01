package dev.morphia.aggregation.expressions.impls;

public interface FieldHolder<T> {
    T field(String name, Expression expression);
}
