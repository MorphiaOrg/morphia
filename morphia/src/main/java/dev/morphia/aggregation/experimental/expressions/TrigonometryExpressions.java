package dev.morphia.aggregation.experimental.expressions;

import java.util.List;

public final class TrigonometryExpressions {
    private TrigonometryExpressions() {
    }

    /**
     * Returns the sine of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/sin $sin
     */
    public static Expression sin(final Expression value) {
        return new Expression("$sin", value);
    }

    /**
     * Returns the cosine of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/cos $cos
     */
    public static Expression cos(final Expression value) {
        return new Expression("$cos", value);
    }

    /**
     * Returns the tangent of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/tan $tan
     */
    public static Expression tan(final Expression value) {
        return new Expression("$tan", value);
    }

    /**
     * Returns the inverse sin (arc sine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/asin $asin
     */
    public static Expression asin(final Expression value) {
        return new Expression("$asin", value);
    }

    /**
     * Returns the inverse cosine (arc cosine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/acos $acos
     */
    public static Expression acos(final Expression value) {
        return new Expression("$acos", value);
    }

    /**
     * Returns the inverse tangent (arc tangent) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/atan $atan
     */
    public static Expression atan(final Expression value) {
        return new Expression("$atan", value);
    }

    /**
     * Returns the inverse tangent (arc tangent) of y / x in radians, where y and x are the first and second values passed to the
     * expression respectively.
     *
     * @param yValue the y value
     * @param xValue the x value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/atan2 $atan2
     */
    public static Expression atan2(final Expression yValue, final Expression xValue) {
        return new Expression("$atan2", List.of(yValue, xValue));
    }

    /**
     * Returns the inverse hyperbolic sine (hyperbolic arc sine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/asinh $asinh
     */
    public static Expression asinh(final Expression value) {
        return new Expression("$asinh", value);
    }

    /**
     * Returns the inverse hyperbolic cosine (hyperbolic arc cosine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/acosh $acosh
     */
    public static Expression acosh(final Expression value) {
        return new Expression("$acosh", value);
    }

    /**
     * Returns the inverse hyperbolic tangent (hyperbolic arc tangent) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/atanh $atanh
     */
    public static Expression atanh(final Expression value) {
        return new Expression("$atanh", value);
    }

    /**
     * Converts a value from degrees to radians.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/degreesToRadians $degreesToRadians
     */
    public static Expression degreesToRadians(final Expression value) {
        return new Expression("$degreesToRadians", value);
    }

    /**
     * Converts a value from radians to degrees.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/radiansToDegrees $radiansToDegrees
     */
    public static Expression radiansToDegrees(final Expression value) {
        return new Expression("$radiansToDegrees", value);
    }

}
