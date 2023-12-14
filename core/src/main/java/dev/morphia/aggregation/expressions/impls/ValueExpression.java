package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Holds a simple value
 * 
 * @hidden
 */
public class ValueExpression extends Expression implements SimpleExpression {
    private final Object object;

    /**
     * @param value the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ValueExpression(@Nullable Object value) {
        super("unused");
        object = value;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the value
     */
    @MorphiaInternal
    @Nullable
    public Object object() {
        return object;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String toString() {
        return "ValueExpression{value=%s}".formatted(object);
    }
}
