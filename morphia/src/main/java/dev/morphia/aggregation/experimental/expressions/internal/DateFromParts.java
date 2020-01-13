package dev.morphia.aggregation.experimental.expressions.internal;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

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
    public DateFromParts day(final int value) {
        return day(literal(value));
    }

    /**
     * Day of month. Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts day(final Expression value) {
        this.day = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts hour(final int value) {
        return hour(literal(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts hour(final Expression value) {
        this.hour = value;
        return this;
    }

    /**
     * Day of week (Monday 1 - Sunday 7).
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoDayOfWeek(final int value) {
        return isoDayOfWeek(literal(value));
    }

    /**
     * Day of week (Monday 1 - Sunday 7). Can be any expression that evaluates to a number.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoDayOfWeek(final Expression value) {
        this.isoDayOfWeek = value;
        return this;
    }

    /**
     * Optional. Can only be used with isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeek(final int value) {
        return isoWeek(literal(value));
    }

    /**
     * Week of year. Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeek(final Expression value) {
        this.isoWeek = value;
        return this;
    }

    /**
     * Required if not using year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeekYear(final int value) {
        return isoWeekYear(literal(value));
    }

    /**
     * ISO Week Date Year. Can be any expression that evaluates to a number.
     * <p>
     * Required if not using year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeekYear(final Expression value) {
        this.isoWeekYear = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts millisecond(final int value) {
        return millisecond(literal(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts millisecond(final Expression value) {
        this.millisecond = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts minute(final int value) {
        return minute(literal(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts minute(final Expression value) {
        this.minute = value;
        return this;
    }

    /**
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts month(final int value) {
        return month(literal(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts month(final Expression value) {
        this.month = value;
        return this;
    }

    /**
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts second(final int value) {
        return second(literal(value));
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts second(final Expression value) {
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
    public DateFromParts timezone(final Expression value) {
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
    public DateFromParts timezone(final String value) {
        return timezone(literal(value));
    }

    /**
     * Calendar year.
     * <p>
     * Required if not using isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts year(final int value) {
        return year(literal(value));
    }

    /**
     * Calendar year. Can be any expression that evaluates to a number.
     * <p>
     * Required if not using isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts year(final Expression value) {
        this.year = value;
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        writer.writeStartDocument();

        ExpressionCodec.writeNamedExpression(mapper, writer, "year", year, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "month", month, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "day", day, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "hour", hour, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "minute", minute, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "second", second, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "millisecond", millisecond, encoderContext);

        ExpressionCodec.writeNamedExpression(mapper, writer, "isoWeekYear", isoWeekYear, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "isoWeek", isoWeek, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "isoDayOfWeek", isoDayOfWeek, encoderContext);

        ExpressionCodec.writeNamedExpression(mapper, writer, "timezone", timezone, encoderContext);

        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
