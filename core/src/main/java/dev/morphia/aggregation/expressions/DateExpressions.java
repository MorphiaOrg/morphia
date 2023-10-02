package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.DateDeltaExpression;
import dev.morphia.aggregation.expressions.impls.DateDiffExpression;
import dev.morphia.aggregation.expressions.impls.DateExpression;
import dev.morphia.aggregation.expressions.impls.DateFromParts;
import dev.morphia.aggregation.expressions.impls.DateFromString;
import dev.morphia.aggregation.expressions.impls.DateToParts;
import dev.morphia.aggregation.expressions.impls.DateToString;
import dev.morphia.aggregation.expressions.impls.DateTruncExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.IsoDates;

/**
 * Defines helper methods for the date expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#date-expression-operators Date Expressions
 * @since 2.0
 */
public final class DateExpressions {
    private DateExpressions() {
    }

    /**
     * Increments a Date object by a specified number of time units.
     *
     * @param startDate The beginning date, in UTC, for the addition operation. The startDate can be any expression that resolves to a
     *                  Date, a Timestamp, or an ObjectID.
     * @param amount    The number of units added to the startDate.
     * @param unit      The unit used to measure the amount of time added to the startDate.
     * @return the new expression
     * @aggregation.expression $dateAdd
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static DateDeltaExpression dateAdd(Expression startDate, long amount, TimeUnit unit) {
        return new DateDeltaExpression("$dateAdd", startDate, amount, unit);
    }

    /**
     * Returns the difference between two dates.
     *
     * @param startDate The beginning date, in UTC, for the addition operation. The startDate can be any expression that resolves to a
     *                  Date, a Timestamp, or an ObjectID.
     * @param endDate   The beginning date, in UTC, for the addition operation. The endDate can be any expression that resolves to a
     *                  Date, a Timestamp, or an ObjectID.
     * @param unit      The unit used to measure the amount of time added to the startDate.
     * @return the new expression
     * @aggregation.expression $dateDiff
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static DateDiffExpression dateDiff(Expression startDate, Expression endDate, TimeUnit unit) {
        return new DateDiffExpression(startDate, endDate, unit);
    }

    /**
     * Constructs and returns a Date object given the date’s constituent properties.
     *
     * @return the new expression
     * @aggregation.expression $dateFromParts
     */
    public static DateFromParts dateFromParts() {
        return new DateFromParts();
    }

    /**
     * Converts a date/time string to a date object.
     *
     * @return the new expression
     * @aggregation.expression $dateFromString
     */
    public static DateFromString dateFromString() {
        return new DateFromString();
    }

    /**
     * Decrements a Date object by a specified number of time units.
     *
     * @param startDate The beginning date, in UTC, for the subtraction operation. The startDate can be any expression that resolves to a
     *                  Date, a Timestamp, or an ObjectID.
     * @param amount    The number of units subtracted to the startDate.
     * @param unit      The unit used to measure the amount of time subtracted to the startDate.
     * @return the new expression
     * @aggregation.expression $dateSubtract
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static DateDeltaExpression dateSubtract(Expression startDate, long amount, TimeUnit unit) {
        return new DateDeltaExpression("$dateSubtract", startDate, amount, unit);
    }

    /**
     * Constructs and returns a Date object given the date’s constituent properties.
     *
     * @param date The input date for which to return parts.
     * @return the new expression
     * @aggregation.expression $dateToParts
     */
    public static DateToParts dateToParts(Expression date) {
        return new DateToParts(date);
    }

    /**
     * Returns the date as a formatted string.
     *
     * @return the new expression
     * @aggregation.expression $dateToString
     */
    public static DateToString dateToString() {
        return new DateToString();
    }

    /**
     * Truncates a date.
     *
     * @param date The date to truncate, specified in UTC. The date can be any expression that resolves to a Date, a Timestamp, or an
     *             ObjectID.
     * @param unit The unit used to measure the amount of time added to the startDate.
     * @return the new expression
     * @aggregation.expression $dateTrunc
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static DateTruncExpression dateTrunc(Expression date, TimeUnit unit) {
        return new DateTruncExpression(date, unit);
    }

    /**
     * Returns the day of the month for a date as a number between 1 and 31.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $dayOfMonth
     */
    public static Expression dayOfMonth(Expression value) {
        return new DateExpression("$dayOfMonth", value);
    }

    /**
     * Returns the day of the week for a date as a number between 1 (Sunday) and 7 (Saturday).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $dayOfWeek
     */
    public static Expression dayOfWeek(Expression value) {
        return new DateExpression("$dayOfWeek", value);
    }

    /**
     * Returns the day of the year for a date as a number between 1 and 366 (leap year).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $dayOfYear
     */
    public static Expression dayOfYear(Expression value) {
        return new DateExpression("$dayOfYear", value);
    }

    /**
     * Returns the hour for a date as a number between 0 and 23.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $hour
     */
    public static Expression hour(Expression value) {
        return new DateExpression("$hour", value);
    }

    /**
     * Returns the weekday number in ISO 8601 format, ranging from 1 (for Monday) to 7 (for Sunday).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $isoDayOfWeek
     */
    public static IsoDates isoDayOfWeek(Expression value) {
        return new IsoDates("$isoDayOfWeek", value);
    }

    /**
     * Returns the week number in ISO 8601 format, ranging from 1 to 53. Week numbers start at 1 with the week (Monday through Sunday) that
     * contains the year’s first Thursday.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $isoWeek
     */
    public static IsoDates isoWeek(Expression value) {
        return new IsoDates("$isoWeek", value);
    }

    /**
     * Returns the year number in ISO 8601 format. The year starts with the Monday of week 1 (ISO 8601) and ends with the Sunday of the
     * last
     * week (ISO 8601).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $isoWeekYear
     */
    public static IsoDates isoWeekYear(Expression value) {
        return new IsoDates("$isoWeekYear", value);
    }

    /**
     * Returns the milliseconds of a date as a number between 0 and 999.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $millisecond
     */
    public static Expression milliseconds(Expression value) {
        return new DateExpression("$millisecond", value);
    }

    /**
     * Returns the minute for a date as a number between 0 and 59.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $minute
     */
    public static Expression minute(Expression value) {
        return new DateExpression("$minute", value);
    }

    /**
     * Returns the month for a date as a number between 1 (January) and 12 (December).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $month
     */
    public static Expression month(Expression value) {
        return new DateExpression("$month", value);
    }

    /**
     * Returns the seconds for a date as a number between 0 and 60 (leap seconds).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $second
     */
    public static Expression second(Expression value) {
        return new DateExpression("$second", value);
    }

    /**
     * Converts a value to a date. If the value cannot be converted to a date, $toDate errors. If the value is null or missing,
     * $toDate returns null.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $toDate
     */
    public static Expression toDate(Expression value) {
        return new DateExpression("$toDate", value);
    }

    /**
     * Returns the incrementing ordinal from a timestamp as a long.
     *
     * @param expression the expression to use when incrementing
     * @return the new expression
     * @aggregation.expression $tsIncrement
     * @mongodb.server.release 5.1
     * @since 2.3
     */
    public static Expression tsIncrement(Expression expression) {
        return new Expression("$tsIncrement", expression);
    }

    /**
     * Returns the seconds from a timestamp as a long.
     *
     * @param expression the expression to use
     * @return the new expression
     * @aggregation.expression $tsSecond
     * @mongodb.server.release 5.1
     * @since 2.3
     */
    public static Expression tsSecond(Expression expression) {
        return new Expression("$tsSecond", expression);
    }

    /**
     * Returns the week number for a date as a number between 0 (the partial week that precedes the first Sunday of the year) and 53
     * (leap year).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $week
     */
    public static Expression week(Expression value) {
        return new DateExpression("$week", value);
    }

    /**
     * Returns the year for a date as a number (e.g. 2014).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @aggregation.expression $year
     */
    public static Expression year(Expression value) {
        return new DateExpression("$year", value);
    }

}
