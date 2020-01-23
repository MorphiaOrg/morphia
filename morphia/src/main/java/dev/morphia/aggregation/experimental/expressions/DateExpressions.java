package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.internal.DateFromParts;
import dev.morphia.aggregation.experimental.expressions.internal.DateFromString;
import dev.morphia.aggregation.experimental.expressions.internal.DateToParts;
import dev.morphia.aggregation.experimental.expressions.internal.DateToString;
import dev.morphia.aggregation.experimental.expressions.internal.IsoDates;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Defines helper methods for the date expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#date-expression-operators Date Expressions
 */
public final class DateExpressions {
    private DateExpressions() {
    }

    /**
     * Converts a value to a date. If the value cannot be converted to a date, $toDate errors. If the value is null or missing,
     * $toDate returns null.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/toDate $toDate
     */
    public static DateExpression toDate(final Expression value) {
        return new DateExpression("$toDate", value);
    }

    /**
     * Returns the day of the month for a date as a number between 1 and 31.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dayOfMonth $dayOfMonth
     */
    public static DateExpression dayOfMonth(final Expression value) {
        return new DateExpression("$dayOfMonth", value);
    }

    /**
     * Returns the day of the year for a date as a number between 1 and 366 (leap year).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dayOfYear $dayOfYear
     */
    public static DateExpression dayOfYear(final Expression value) {
        return new DateExpression("$dayOfYear", value);
    }

    /**
     * Returns the day of the week for a date as a number between 1 (Sunday) and 7 (Saturday).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dayOfWeek $dayOfWeek
     */
    public static DateExpression dayOfWeek(final Expression value) {
        return new DateExpression("$dayOfWeek", value);
    }

    /**
     * Returns the month for a date as a number between 1 (January) and 12 (December).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/month $month
     */
    public static DateExpression month(final Expression value) {
        return new DateExpression("$month", value);
    }

    /**
     * Returns the hour for a date as a number between 0 and 23.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/hour $hour
     */
    public static DateExpression hour(final Expression value) {
        return new DateExpression("$hour", value);
    }

    /**
     * Returns the minute for a date as a number between 0 and 59.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/minute $minute
     */
    public static DateExpression minute(final Expression value) {
        return new DateExpression("$minute", value);
    }

    /**
     * Returns the seconds for a date as a number between 0 and 60 (leap seconds).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/second $second
     */
    public static DateExpression second(final Expression value) {
        return new DateExpression("$second", value);
    }

    /**
     * Returns the week number for a date as a number between 0 (the partial week that precedes the first Sunday of the year) and 53
     * (leap year).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/week $week
     */
    public static DateExpression week(final Expression value) {
        return new DateExpression("$week", value);
    }

    /**
     * Returns the milliseconds of a date as a number between 0 and 999.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/millisecond $millisecond
     */
    public static DateExpression milliseconds(final Expression value) {
        return new DateExpression("$millisecond", value);
    }

    /**
     * Returns the year for a date as a number (e.g. 2014).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/year $year
     */
    public static DateExpression year(final Expression value) {
        return new DateExpression("$year", value);
    }

    /**
     * Returns the date as a formatted string.
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dateToString $dateToString
     */
    public static DateToString dateToString() {
        return new DateToString();
    }

    /**
     * Converts a date/time string to a date object.
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dateFromString $dateFromString
     */
    public static DateFromString dateFromString() {
        return new DateFromString();
    }

    /**
     * Constructs and returns a Date object given the date’s constituent properties.
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dateFromParts $dateFromParts
     */
    public static DateFromParts dateFromParts() {
        return new DateFromParts();
    }

    /**
     * Returns the weekday number in ISO 8601 format, ranging from 1 (for Monday) to 7 (for Sunday).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/isoDayOfWeek $isoDayOfWeek
     */
    public static IsoDates isoDayOfWeek(final Expression value) {
        return new IsoDates("$isoDayOfWeek", value);
    }

    /**
     * Returns the week number in ISO 8601 format, ranging from 1 to 53. Week numbers start at 1 with the week (Monday through Sunday) that
     * contains the year’s first Thursday.
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/isoWeek $isoWeek
     */
    public static IsoDates isoWeek(final Expression value) {
        return new IsoDates("$isoWeek", value);
    }

    /**
     * Returns the year number in ISO 8601 format. The year starts with the Monday of week 1 (ISO 8601) and ends with the Sunday of the
     * last
     * week (ISO 8601).
     *
     * @param value the expression containing the date value
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/isoWeekYear $isoWeekYear
     */
    public static IsoDates isoWeekYear(final Expression value) {
        return new IsoDates("$isoWeekYear", value);
    }

    /**
     * Constructs and returns a Date object given the date’s constituent properties.
     *
     * @param date The input date for which to return parts.
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dateToParts $dateToParts
     */
    public static DateToParts dateToParts(final Expression date) {
        return new DateToParts(date);
    }

    /**
     * Base class for the date expressions
     *
     * @mongodb.driver.manual reference/operator/aggregation/#date-expression-operators Date Expressions
     */
    public static class DateExpression extends Expression {
        protected DateExpression(final String operation, final Expression value) {
            super(operation, value);
        }

        @Override
        public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeName(getOperation());
            ExpressionCodec.writeUnnamedExpression(mapper, writer, (Expression) getValue(), encoderContext);
            writer.writeEndDocument();
        }
    }
}
