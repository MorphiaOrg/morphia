package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;

/**
 * Adds new fields to documents. $set outputs documents that contain all existing fields from the input documents and newly added fields.
 * <p>
 * The $set stage is an alias for $addFields.
 * <p>
 * Both stages are equivalent to a $project stage that explicitly specifies all existing fields in the input documents and adds the new
 * fields.
 *
 * @aggregation.expression $set
 * @since 2.3
 */
public class Set extends Stage {
    private final DocumentExpression document = Expressions.of();

    protected Set() {
        super("$set");
    }

    /**
     * Creates a new Set stage
     *
     * @return the new stage
     */
    public static Set set() {
        return new Set();
    }

    /**
     * Add a field to the stage
     *
     * @param name  the name of the new field
     * @param value the value expression
     * @return this
     */
    public Set field(String name, Expression value) {
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
