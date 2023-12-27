package dev.morphia.aggregation.expressions.impls;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;

import static java.util.Arrays.asList;

/**
 * Wraps a list of expressions as an Expression so we can generically deal with all values as Expressions
 *
 * @hidden
 * @morphia.internal
 * @since 2.3
 */
@MorphiaInternal
public class ExpressionList extends Expression implements SimpleExpression {
    private final List<Expression> values;

    /**
     * @param values the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExpressionList(List<Expression> values) {
        super("unused");
        this.values = new ArrayList<>(values);
    }

    /**
     * @param values the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExpressionList(Expression... values) {
        super("unused");
        this.values = new ArrayList<>(asList(values));
    }

    /**
     * @param expression the expression
     */
    public void add(Expression expression) {
        values.add(expression);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Override
    @MorphiaInternal
    public Expression value() {
        throw new UnsupportedOperationException("should have called values() here");
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the value
     */
    @NonNull
    @MorphiaInternal
    public List<Expression> values() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
