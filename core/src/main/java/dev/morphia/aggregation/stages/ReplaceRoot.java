package dev.morphia.aggregation.stages;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Replaces the input document with the specified document. The operation replaces all existing fields in the input document, including
 * the _id field.
 *
 * @aggregation.stage $replaceRoot
 */
public class ReplaceRoot extends Stage {
    private Expression value;
    private DocumentExpression document;

    /**
     * @param expression the expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected ReplaceRoot(Expression expression) {
        this();
        this.value = expression;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected ReplaceRoot() {
        super("$replaceRoot");
    }

    /**
     * Creates a new stage
     *
     * @return the new stage
     * @since 2.2
     * @aggregation.stage $replaceRoot
     * @mongodb.server.release 3.4
     */
    public static ReplaceRoot replaceRoot() {
        return new ReplaceRoot();
    }

    /**
     * Creates a new stage to replace the root with the given expression. This expression must evaluate to a document. No further
     * fields can be added to this stage.
     *
     * @param expression the document expression
     * @return the new stage
     * @since 2.2
     */
    public static ReplaceRoot replaceRoot(Object expression) {
        return new ReplaceRoot(wrap(expression));
    }

    /**
     * Adds a new field
     *
     * @param name       the field name
     * @param expression the value expression
     * @return this
     */
    public ReplaceRoot field(String name, Expression expression) {
        if (value != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(stageName()));
        }
        if (document == null) {
            document = Expressions.document();
        }
        document.field(name, expression);

        return this;
    }

    /**
     * @return the expression
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public DocumentExpression getDocument() {
        return document;
    }

    /**
     * @return the expression
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Expression getValue() {
        return value;
    }
}
