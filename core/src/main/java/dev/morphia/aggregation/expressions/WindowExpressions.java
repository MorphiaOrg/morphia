package dev.morphia.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.expressions.impls.Accumulator;
import dev.morphia.aggregation.expressions.impls.CalculusExpression;
import dev.morphia.aggregation.expressions.impls.DenseRankExpression;
import dev.morphia.aggregation.expressions.impls.DocumentNumberExpression;
import dev.morphia.aggregation.expressions.impls.ExpMovingAvg;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.MathExpression;
import dev.morphia.aggregation.expressions.impls.RankExpression;
import dev.morphia.aggregation.expressions.impls.ShiftExpression;
import dev.morphia.aggregation.stages.SetWindowFields;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Provides window specific operations.
 *
 * @since 2.3
 */
public final class WindowExpressions {
    private WindowExpressions() {
    }

    /**
     * Returns the population covariance of two numeric expressions that are evaluated using documents in the $setWindowFields stage window.
     * <p>
     * $covariancePop is only available in the $setWindowFields stage.
     *
     * @param first  the first value to evaluate
     * @param second the second value to evaluate
     * @return the new expression
     * @mongodb.server.release 5.0
     * @aggregation.expression $covariancePop
     * @see Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static Expression covariancePop(Object first, Object second) {
        return new MathExpression("$covariancePop", wrap(List.of(first, second)));
    }

    /**
     * Returns the sample covariance of two numeric expressions that are evaluated using documents in the $setWindowFields stage window.
     * <p>
     * $covarianceSamp is only available in the $setWindowFields stage.
     *
     * @param first  the first value to evaluate
     * @param second the second value to evaluate
     * @return the new expression
     * @mongodb.server.release 5.0
     * @aggregation.expression $covarianceSamp
     * @see Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static Expression covarianceSamp(Object first, Object second) {
        return new MathExpression("$covarianceSamp", wrap(List.of(first, second)));
    }

    /**
     * Returns the document position (known as the rank) relative to other documents in the $setWindowFields stage partition.
     *
     * @return the expression
     * @mongodb.server.release 5.0
     * @aggregation.expression $denseRank
     * @since 2.3
     */
    public static Expression denseRank() {
        return new DenseRankExpression();
    }

    /**
     * Returns the average rate of change within the specified window.
     *
     * @param input Specifies the value to evaluate. If an expression, it must evaluate to a number.
     * @return the new expression
     * @aggregation.expression $derivative
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static CalculusExpression derivative(Object input) {
        return new CalculusExpression("$derivative", wrap(input));
    }

    /**
     * Returns the position of a document (known as the document number) in the $setWindowFields stage partition.
     *
     * @return the new expression
     * @aggregation.expression $documentNumber
     * @mongodb.server.release 5.0
     * @see Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static Expression documentNumber() {
        return new DocumentNumberExpression();
    }

    /**
     * Returns the exponential moving average of numeric expressions applied to documents in a partition defined in the $setWindowFields
     * stage.
     * <p>
     * $expMovingAvg is only available in the $setWindowFields stage.
     *
     * @param input Specifies the value to evaluate. Non-numeric expressions are ignored.
     * @param n     An integer that specifies the number of historical documents that have a significant mathematical weight in the
     *              exponential moving average calculation, with the most recent documents contributing the most weight.
     * @return the new expression
     * @mongodb.server.release 5.0
     * @aggregation.expression $expMovingAvg
     * @see Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static Expression expMovingAvg(Object input, int n) {
        return new ExpMovingAvg(wrap(input), n);
    }

    /**
     * Returns the exponential moving average of numeric expressions applied to documents in a partition defined in the $setWindowFields
     * stage.
     * <p>
     * $expMovingAvg is only available in the $setWindowFields stage.
     *
     * @param input Specifies the expression to evaluate. Non-numeric expressions are ignored.
     * @param alpha A double that specifies the exponential decay value to use in the exponential moving average calculation. A higher
     *              alpha value assigns a lower mathematical significance to previous results from the calculation.
     * @return the new expression
     * @mongodb.server.release 5.0
     * @aggregation.expression $expMovingAvg
     * @see Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static Expression expMovingAvg(Object input, double alpha) {
        return new ExpMovingAvg(wrap(input), alpha);
    }

    /**
     * Returns the approximation of the area under a curve, which is calculated using the trapezoidal rule where each set of adjacent
     * documents form a trapezoid using the:
     *
     * @param input Specifies the value to evaluate. If an expression, it must evaluate to a number.
     * @return the new expression
     * @aggregation.expression $integral
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static CalculusExpression integral(Object input) {
        return new CalculusExpression("$integral", wrap(input));
    }

    /**
     * Fills null and missing fields in a window using linear interpolation based on surrounding field values.
     * <p>
     * $linearFill is only available in the $setWindowFields stage.
     *
     * @param fillValue the expression to use when calculating fill values
     * @return the fill expression
     * @mongodb.server.release 5.3
     * @aggregation.expression $linearFill
     * @since 2.3
     */
    public static Expression linearFill(Object fillValue) {
        return new Expression("$linearFill", wrap(fillValue));
    }

    /**
     * Last observation carried forward. Set values for null and missing fields in a window to the last non-null value for the field.
     *
     * @param fillValue the expression to use when calculating fill values
     * @return the fill expression
     * @mongodb.server.release 5.2
     * @aggregation.expression $locf
     * @since 2.3
     */
    public static Expression locf(Object fillValue) {
        return new Expression("$locf", wrap(fillValue));
    }

    /**
     * Returns the document position (known as the rank) relative to other documents in the $setWindowFields stage partition.
     *
     * @return the new expression
     * @aggregation.expression $rank
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static Expression rank() {
        return new RankExpression();
    }

    /**
     * Returns the value from an expression applied to a document in a specified position relative to the current document in the
     * $setWindowFields stage partition.
     *
     * @param output       Specifies an expression to evaluate and return in the output.
     * @param by           Specifies an integer with a numeric document position relative to the current document in the output.
     * @param defaultValue Specifies an optional default expression to evaluate if the document position is outside of the implicit
     *                     $setWindowFields stage window. The implicit window contains all the documents in the partition.
     * @return the expression
     * @aggregation.expression $shift
     * @mongodb.server.release 5.0
     * @see Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static Expression shift(Object output, long by, Object defaultValue) {
        return new ShiftExpression(wrap(output), by, wrap(defaultValue));
    }

    /**
     * Returns the population standard deviation of the input values.
     *
     * @param value      the value
     * @param additional any subsequent expressions to include in the expression
     * @return the new expression
     * @aggregation.expression $stdDevPop
     * @since 2.3
     */
    public static Expression stdDevPop(Object value, Object... additional) {
        return new Accumulator("$stdDevPop", wrap(value, additional));
    }

    /**
     * Returns the sample standard deviation of the input values.
     *
     * @param value      the value
     * @param additional any subsequent expressions to include in the expression
     * @return the new expression
     * @aggregation.expression $stdDevSamp
     * @since 2.3
     */
    public static Expression stdDevSamp(Object value, Object... additional) {
        return new Accumulator("$stdDevSamp", wrap(value, additional));
    }

}
