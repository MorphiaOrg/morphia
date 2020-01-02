package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PipelineField {
    private String name;
    private Expression value;

    PipelineField(final String name, final Expression value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }
}
