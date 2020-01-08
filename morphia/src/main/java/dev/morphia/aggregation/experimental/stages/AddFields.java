package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Expression.DocumentExpression;

public class AddFields extends Stage {
    private DocumentExpression document = Expression.of();
    protected AddFields() {
        super("$addFields");
    }

    public static AddFields of() {
        return new AddFields();
    }

    public AddFields field(final String name, final Expression value) {
        document.field(name, value);
        return this;
    }

    /**
     * @return the fields
     *
     * @morphia.internal
     */
    public DocumentExpression getDocument() {
        return document;
    }
}
