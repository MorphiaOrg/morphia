package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

import static dev.morphia.mapping.codec.CodecHelper.coalesce;

/**
 * Evaluates an expression and returns the value of the expression if the expression evaluates to a non-null value. If the
 * expression evaluates to a null value, including instances of undefined values or missing fields, returns the value of the
 * replacement expression.
 */
public class IfNull extends Expression implements FieldHolder<IfNull> {
    private List<Expression> input;
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

    public List<Expression> input() {
        return input;
    }

    public IfNull input(Expression input, Expression... inputs) {
        this.input = coalesce(input, inputs);
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
        this.input = List.of(target);
        return this;
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
