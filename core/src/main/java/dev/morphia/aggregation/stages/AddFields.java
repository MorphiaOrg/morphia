package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

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
    private final DocumentExpression document = Expressions.document();

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DocumentExpression getDocument() {
        return document;
    }
}
