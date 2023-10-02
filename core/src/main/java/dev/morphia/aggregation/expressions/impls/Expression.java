package dev.morphia.aggregation.expressions.impls;

import java.util.List;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base class for all the expression types.
 *
 * @mongodb.driver.manual reference/operator/aggregation/ Expressions
 * @since 2.0
 */
public class Expression {
    private final String operation;
    private final Expression value;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression(String operation) {
        this.operation = operation;
        this.value = null;
    }

    /**
     * @param operation the expression name
     * @param value     the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression(String operation, Expression value) {
        this.operation = operation;
        this.value = value;
    }

    /**
     * @param operation the expression name
     * @param value     the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression(String operation, List<Expression> value) {
        this.operation = operation;
        this.value = new ExpressionList(value);
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String operation() {
        return operation;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Expression value() {
        return value;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String toString() {
        return new StringJoiner(", ", Expression.class.getSimpleName() + "[", "]")
                .add("operation='" + operation + "'")
                .toString();
    }
}
