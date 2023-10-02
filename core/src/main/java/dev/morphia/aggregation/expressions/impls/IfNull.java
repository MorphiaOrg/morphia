package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

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
            document = Expressions.of();
        }
        document.field(name, expression);

        return this;
    }

    public IfNull replacement(Expression replacement) {
        this.replacement = replacement;
        return this;
    }

    public IfNull target(Expression target) {
        this.target = target;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression target() {
        return target;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression replacement() {
        return replacement;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DocumentExpression document() {
        return document;
    }
}
