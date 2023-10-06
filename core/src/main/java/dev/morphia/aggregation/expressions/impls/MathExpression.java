package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base class for the math expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#arithmetic-expression-operators Arithmetic Expressions
 * @since 2.0
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class MathExpression extends Expression {

    /**
     * @param operation the operation
     * @param operands  the operands
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MathExpression(String operation, List<Expression> operands) {
        super(operation, new ExpressionList(operands));
    }

    /**
     * @param operation the operation
     * @param operand   the operand
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MathExpression(String operation, Expression operand) {
        super(operation, new ExpressionList(operand));
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public ExpressionList value() {
        return (ExpressionList) super.value();
    }
}
