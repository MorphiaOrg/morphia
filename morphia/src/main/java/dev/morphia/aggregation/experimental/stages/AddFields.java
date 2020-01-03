package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;

import java.util.ArrayList;
import java.util.List;

public class AddFields extends Stage {
    private List<PipelineField> fields = new ArrayList<>();
    protected AddFields() {
        super("$addFields");
    }

    public static AddFields of() {
        return new AddFields();
    }

    public AddFields field(final String name, final Expression value) {
        fields.add(new PipelineField(name, value));
        return this;
    }

    /**
     * @return the fields
     *
     * @morphia.internal
     */
    public List<PipelineField> getFields() {
        return fields;
    }
}
