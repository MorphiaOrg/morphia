package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.Expressions;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class DateFromParts extends Expression {
    private Expression year;
    private Expression month;
    private Expression day;
    private Expression hour;
    private Expression minute;
    private Expression second;
    private Expression millisecond;

    private Expression isoWeekYear;
    private Expression isoWeek;
    private Expression isoDayOfWeek;

    private Expression timezone;

    public DateFromParts() {
        super("$dateFromParts");
    }

    /**
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts day(int value) {
        return day(Expressions.value(value));
    }

    /**
     * Day of month. Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts day(Expression value) {
        this.day = value;
        return this;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "year", year, encoderContext);
            expression(datastore, writer, "month", month, encoderContext);
            expression(datastore, writer, "day", day, encoderContext);
            expression(datastore, writer, "hour", hour, encoderContext);
            expression(datastore, writer, "minute", minute, encoderContext);
            expression(datastore, writer, "second", second, encoderContext);
            expression(datastore, writer, "millisecond", millisecond, encoderContext);
            expression(datastore, writer, "isoWeekYear", isoWeekYear, encoderContext);
            expression(datastore, writer, "isoWeek", isoWeek, encoderContext);
            expression(datastore, writer, "isoDayOfWeek", isoDayOfWeek, encoderContext);
            expression(datastore, writer, "timezone", timezone, encoderContext);
        });
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts hour(int value) {
        return hour(Expressions.value(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts hour(Expression value) {
        this.hour = value;
        return this;
    }

    /**
     * Day of week (Monday 1 - Sunday 7).
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoDayOfWeek(int value) {
        return isoDayOfWeek(Expressions.value(value));
    }

    /**
     * Day of week (Monday 1 - Sunday 7). Can be any expression that evaluates to a number.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoDayOfWeek(Expression value) {
        this.isoDayOfWeek = value;
        return this;
    }

    /**
     * Optional. Can only be used with isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeek(int value) {
        return isoWeek(Expressions.value(value));
    }

    /**
     * Week of year. Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeek(Expression value) {
        this.isoWeek = value;
        return this;
    }

    /**
     * Required if not using year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeekYear(int value) {
        return isoWeekYear(Expressions.value(value));
    }

    /**
     * ISO Week Date Year. Can be any expression that evaluates to a number.
     * <p>
     * Required if not using year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeekYear(Expression value) {
        this.isoWeekYear = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts millisecond(int value) {
        return millisecond(Expressions.value(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts millisecond(Expression value) {
        this.millisecond = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts minute(int value) {
        return minute(Expressions.value(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts minute(Expression value) {
        this.minute = value;
        return this;
    }

    /**
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts month(int value) {
        return month(Expressions.value(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts month(Expression value) {
        this.month = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts second(int value) {
        return second(Expressions.value(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts second(Expression value) {
        this.second = value;
        return this;
    }

    /**
     * Can be any expression that evaluates to a string whose value is either:
     *
     * <ul>
     *     <li>an Olson Timezone Identifier, such as "Europe/London" or "America/New_York", or
     *     <li>a UTC offset in the form:
     *     <ul>
     *         <li> +/-[hh]:[mm], e.g. "+04:45", or
     *         <li> +/-[hh][mm], e.g. "-0530", or
     *         <li> +/-[hh], e.g. "+03".
     *     </ul>
     * </ul>
     * <p>
     * Optional
     */
    public DateFromParts timezone(Expression value) {
        this.timezone = value;
        return this;
    }

    /**
     * Can be any string whose value is either:
     *
     * <ul>
     *     <li>an Olson Timezone Identifier, such as "Europe/London" or "America/New_York", or
     *     <li>a UTC offset in the form:
     *     <ul>
     *         <li> +/-[hh]:[mm], e.g. "+04:45", or
     *         <li> +/-[hh][mm], e.g. "-0530", or
     *         <li> +/-[hh], e.g. "+03".
     *     </ul>
     * </ul>
     * <p>
     * Optional
     */
    public DateFromParts timezone(String value) {
        return timezone(Expressions.value(value));
    }

    /**
     * Calendar year.
     * <p>
     * Required if not using isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts year(int value) {
        return year(Expressions.value(value));
    }

    /**
     * Calendar year. Can be any expression that evaluates to a number.
     * <p>
     * Required if not using isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts year(Expression value) {
        this.year = value;
        return this;
    }
}
