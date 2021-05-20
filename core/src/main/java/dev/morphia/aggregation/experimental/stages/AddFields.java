package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;

/**
 * Adds new fields to documents. $addFields outputs documents that contain all existing fields from the input documents and newly added
 * fields.
 * <p>
 * The $addFields stage is equivalent to a $project stage that explicitly specifies all existing fields in the input documents and adds
 * the new fields.
 *
 * @aggregation.expression $addFields
 */
public class AddFields extends Stage {
    private final DocumentExpression document = Expressions.of();

    protected AddFields() {
        super("$addFields");
    }

    /**
     * Creates a new AddFields stage
     *
     * @return the new stage
     * @since 2.2
     */
    public static AddFields addFields() {
        return new AddFields();
    }

    /**
     * Creates a new AddFields stage to bind field
     *
     * @return the new stage
     * @deprecated use {@link #addFields()}
     */
    @Deprecated(forRemoval = true)
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
    public AddFields field(String name, Expression value) {
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
