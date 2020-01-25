package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;

/**
 * Adds new fields to documents. $addFields outputs documents that contain all existing fields from the input documents and newly added
 * fields.
 * <p>
 * The $addFields stage is equivalent to a $project stage that explicitly specifies all existing fields in the input documents and adds
 * the new fields.
 *
 * @mongodb.driver.manual reference/operator/aggregation/addFields/ $addFields
 */
public class AddFields extends Stage {
    private DocumentExpression document = Expression.of();

    protected AddFields() {
        super("$addFields");
    }

    /**
     * Creates a new AddFields stage to bind field
     *
     * @return the new stage
     */
    public static AddFields of() {
        return new AddFields();
    }

    /**
     * Add a field to the stage
     *
     * @param name  the name of the new field
     * @param value the value expression
     * @return this
     */
    public AddFields field(final String name, final Expression value) {
        document.field(name, value);
        return this;
    }

    /**
     * @return the fields
     * @morphia.internal
     */
    public DocumentExpression getDocument() {
        return document;
    }
}
