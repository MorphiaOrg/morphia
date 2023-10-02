package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
@MorphiaInternal
public class PipelineField {
    private final String name;
    private final Expression value;

    public PipelineField(String name, Expression value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public String toString() {
        return format("PipelineField{name='%s', value=%s}", name, value);
    }
}
