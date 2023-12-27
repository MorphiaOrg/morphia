package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

import static java.lang.String.format;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class PipelineField {
    private final String name;
    private final Expression value;

    /**
     * @param name  the name
     * @param value the value
     */
    public PipelineField(String name, Object value) {
        this.name = name;
        this.value = Expressions.wrap(value);
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the name
     */
    @MorphiaInternal
    public String name() {
        return name;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the value
     */
    @MorphiaInternal
    public Expression value() {
        return value;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Override
    @MorphiaInternal
    public String toString() {
        return format("PipelineField{name='%s', value=%s}", name, value);
    }
}
