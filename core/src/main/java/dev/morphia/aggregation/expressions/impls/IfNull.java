package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

/**
 * Evaluates an expression and returns the value of the expression if the expression evaluates to a non-null value. If the
 * expression evaluates to a null value, including instances of undefined values or missing fields, returns the value of the
 * replacement expression.
 */
public class IfNull extends Expression implements FieldHolder<IfNull> {
    private Expression target;
    private Expression replacement;
    private DocumentExpression document;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public IfNull() {
        super("$ifNull");
    }

    @Override
    public IfNull field(String name, Expression expression) {
        if (replacement != null) {
            throw new AggregationException(Sofia.mixedModesNotAllowed(operation()));
        }
        if (document == null) {
            document = Expressions.document();
        }
        document.field(name, expression);

        return this;
    }

    /**
     * @param replacement the replacement
     * @return this
     */
    public IfNull replacement(Expression replacement) {
        this.replacement = replacement;
        return this;
    }

    /**
     * @param target the target
     * @return this
     */
    public IfNull target(Expression target) {
        this.target = target;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the target
     */
    @MorphiaInternal
    public Expression target() {
        return target;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the replacement
     */
    @MorphiaInternal
    public Expression replacement() {
        return replacement;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the document
     */
    @MorphiaInternal
    public DocumentExpression document() {
        return document;
    }
}
