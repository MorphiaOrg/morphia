package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Fields;

public class AddFields extends Stage {
    private Fields<AddFields> fields = Expression.fields(this);
    protected AddFields() {
        super("$addFields");
    }

    public static AddFields of() {
        return new AddFields();
    }

    public AddFields field(final String name, final Expression value) {
        fields.add(name, value);
        return this;
    }

    /**
     * @return the fields
     *
     * @morphia.internal
     */
    public Fields<AddFields> getFields() {
        return fields;
    }
}
