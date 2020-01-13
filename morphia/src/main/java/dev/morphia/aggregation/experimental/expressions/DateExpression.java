package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.internal.DateFromParts;
import dev.morphia.aggregation.experimental.expressions.internal.DateFromString;
import dev.morphia.aggregation.experimental.expressions.internal.DateToString;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class DateExpression extends Expression {

    protected DateExpression(final String operation, final Expression value) {
        super(operation, value);
    }

    /**
     * Converts a value to a date. If the value cannot be converted to a date, $toDate errors. If the value is null or missing,
     * $toDate returns null.
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/toDate $toDate
     */
    public static DateExpression toDate(Expression value) {
        return new DateExpression("$toDate", value);
    }

    /**
     * Returns the day of the month for a date as a number between 1 and 31.
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dayOfMonth $dayOfMonth
     */
    public static DateExpression dayOfMonth(Expression value) {
        return new DateExpression("$dayOfMonth", value);
    }

    /**
     * Returns the day of the year for a date as a number between 1 and 366 (leap year).
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dayOfYear $dayOfYear
     */
    public static DateExpression dayOfYear(Expression value) {
        return new DateExpression("$dayOfYear", value);
    }

    /**
     * Returns the day of the week for a date as a number between 1 (Sunday) and 7 (Saturday).
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/dayOfWeek $dayOfWeek
     */
    public static DateExpression dayOfWeek(Expression value) {
        return new DateExpression("$dayOfWeek", value);
    }

    /**
     * Returns the month for a date as a number between 1 (January) and 12 (December).
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/month $month
     */
    public static DateExpression month(Expression value) {
        return new DateExpression("$month", value);
    }

    /**
     * Returns the hour for a date as a number between 0 and 23.
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/hour $hour
     */
    public static DateExpression hour(Expression value) {
        return new DateExpression("$hour", value);
    }

    /**
     * Returns the minute for a date as a number between 0 and 59.
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/minute $minute
     */
    public static DateExpression minute(Expression value) {
        return new DateExpression("$minute", value);
    }

    /**
     * Returns the seconds for a date as a number between 0 and 60 (leap seconds).
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/second $second
     */
    public static DateExpression second(Expression value) {
        return new DateExpression("$second", value);
    }

    /**
     * Returns the week number for a date as a number between 0 (the partial week that precedes the first Sunday of the year) and 53
     *  (leap year).
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/week $week
     */
    public static DateExpression week(Expression value) {
        return new DateExpression("$week", value);
    }

    /**
     * Returns the milliseconds of a date as a number between 0 and 999.
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/millisecond $millisecond
     */
    public static DateExpression milliseconds(Expression value) {
        return new DateExpression("$millisecond", value);
    }

    /**
     * Returns the year for a date as a number (e.g. 2014).
     *
     * @param value the expression containing the date value
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/year $year
     */
    public static DateExpression year(Expression value) {
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

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(operation);
        ExpressionCodec.writeUnnamedExpression(mapper, writer, (Expression) value, encoderContext);
        writer.writeEndDocument();
    }

}
