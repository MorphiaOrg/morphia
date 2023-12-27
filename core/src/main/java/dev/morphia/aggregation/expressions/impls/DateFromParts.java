package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Constructs and returns a Date object given the date’s constituent properties.
 */
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

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateFromParts() {
        super("$dateFromParts");
    }

    /**
     * Day of month. Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts day(Object value) {
        this.day = Expressions.wrap(value);
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the year
     */
    @MorphiaInternal
    public Expression year() {
        return year;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the month
     */
    @MorphiaInternal
    public Expression month() {
        return month;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the day
     */
    @MorphiaInternal
    public Expression day() {
        return day;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the hour
     */
    @MorphiaInternal
    public Expression hour() {
        return hour;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the minute
     */
    @MorphiaInternal
    public Expression minute() {
        return minute;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the second
     */
    @MorphiaInternal
    public Expression second() {
        return second;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the milliseconds
     */
    @MorphiaInternal
    public Expression millisecond() {
        return millisecond;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the week of the year
     */
    @MorphiaInternal
    public Expression isoWeekYear() {
        return isoWeekYear;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the week
     */
    @MorphiaInternal
    public Expression isoWeek() {
        return isoWeek;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the day of the week
     */
    @MorphiaInternal
    public Expression isoDayOfWeek() {
        return isoDayOfWeek;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the timezone
     */
    @MorphiaInternal
    public Expression timezone() {
        return timezone;
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts hour(Object value) {
        this.hour = Expressions.wrap(value);
        return this;
    }

    /**
     * Day of week (Monday 1 - Sunday 7). Can be any expression that evaluates to a number.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoDayOfWeek(Object value) {
        this.isoDayOfWeek = Expressions.wrap(value);
        return this;
    }

    /**
     * Week of year. Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeek(Object value) {
        this.isoWeek = Expressions.wrap(value);
        return this;
    }

    /**
     * ISO Week Date Year. Can be any expression that evaluates to a number.
     * <p>
     * Required if not using year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts isoWeekYear(Object value) {
        this.isoWeekYear = Expressions.wrap(value);
        return this;
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts millisecond(Object value) {
        this.millisecond = Expressions.wrap(value);
        return this;
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts minute(Object value) {
        this.minute = Expressions.wrap(value);
        return this;
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional. Can only be used with year.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts month(Object value) {
        this.month = Expressions.wrap(value);
        return this;
    }

    /**
     * Can be any expression that evaluates to a number.
     * <p>
     * Optional
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts second(Object value) {
        this.second = Expressions.wrap(value);
        return this;
    }

    /**
     * Can be any expression that evaluates to a string whose value is either:
     *
     * <ul>
     * <li>an Olson Timezone Identifier, such as "Europe/London" or "America/New_York", or
     * <li>a UTC offset in the form:
     * <ul>
     * <li>+/-[hh]:[mm], e.g. "+04:45", or
     * <li>+/-[hh][mm], e.g. "-0530", or
     * <li>+/-[hh], e.g. "+03".
     * </ul>
     * </ul>
     * <p>
     * Optional
     * 
     * @param value the timezone
     * @return this
     */
    public DateFromParts timezone(Object value) {
        this.timezone = Expressions.wrap(value);
        return this;
    }

    /**
     * Calendar year. Can be any expression that evaluates to a number.
     * <p>
     * Required if not using isoWeekYear.
     *
     * @param value the value to use
     * @return this
     */
    public DateFromParts year(Object value) {
        this.year = Expressions.wrap(value);
        return this;
    }
}
