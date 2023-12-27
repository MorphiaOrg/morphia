package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 * @param <T> the type of the FieldHolder
 */
@MorphiaInternal
public interface FieldHolder<T> {
    /**
     * @param name       the field name
     * @param expression the field value
     * @return this
     */
    T field(String name, Object expression);
}
