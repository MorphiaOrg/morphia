package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.sofia.Sofia;

/**
 * Replaces the input document with the specified document. The operation replaces all existing fields in the input document, including
 * the _id field. With $replaceWith, you can promote an embedded document to the top-level. You can also specify a new document as the
 * replacement.
 * <p>
 * The $replaceWith is an alias for $replaceRoot.
 *
 * @mongodb.driver.manual reference/operator/aggregation/replaceWith/ $replaceWith
 */
public class ReplaceWith extends Stage {
    private Expression value;
    private DocumentExpression document;

    protected ReplaceWith(final Expression expression) {
        this();
        this.value = expression;
    }

    protected ReplaceWith() {
        super("$replaceWith");
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     */
    public static ReplaceWith with() {
        return new ReplaceWith();
    }

    /**
     * Creates a new stage to replace the root with the given expression.  This expression must evaluate to a document.  No further
     * fields can be added to this stage.
     *
     * @param expression the document expression
     * @return the new stage
     */
    public static ReplaceWith with(final Expression expression) {
        return new ReplaceWith(expression);
    }

    /**
     * Adds a new field
     *
     * @param name       the field name
     * @param expression the value expression
     * @return this
     */
    public ReplaceWith field(final String name, final Expression expression) {
        if (value != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(getStageName()));
        }
        if (document == null) {
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
    }

    /**
     * @return the expression
     * @morphia.internal
     */
    public DocumentExpression getDocument() {
        return document;
    }

    /**
     * @return the expression
     * @morphia.internal
     */
    public Expression getValue() {
        return value;
    }
}
