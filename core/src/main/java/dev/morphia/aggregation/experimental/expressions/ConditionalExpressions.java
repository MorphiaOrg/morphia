package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.IfNull;
import dev.morphia.aggregation.experimental.expressions.impls.SwitchExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;

/**
 * Defines helper methods for the conditional expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#conditional-expression-operators Conditional Expressions
 * @since 2.0
 */
public class ConditionalExpressions {
    private ConditionalExpressions() {
    }

    /**
     * Evaluates a boolean expression to return one of the two specified return expressions.
     *
     * @param condition the condition to evaluate
     * @param then      the expression for the true branch
     * @param otherwise the expresion for the else branch
     * @return the new expression
     * @aggregation.expression $cond
     */
    public static Expression condition(Expression condition, Expression then, Expression otherwise) {
        return new Expression("$cond", List.of(condition, then, otherwise)) {
            @Override
            public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
                array(datastore, writer, getOperation(), (List<Expression>) getValue(), encoderContext);
            }
        };
    }

    /**
     * Evaluates an expression and returns the value of the expression if the expression evaluates to a non-null value. If the
     * expression evaluates to a null value, including instances of undefined values or missing fields, returns the value of the
     * replacement expression.
     *
     * @return the new expression
     * @aggregation.expression $ifNull
     */
    public static IfNull ifNull() {
        return new IfNull();
    }

    /**
     * Evaluates a series of case expressions. When it finds an expression which evaluates to true, $switch executes a specified
     * expression and breaks out of the control flow.
     *
     * @return the new expression
     * @aggregation.expression $switch
     */
    public static SwitchExpression switchExpression() {
        return new SwitchExpression();
    }
}
