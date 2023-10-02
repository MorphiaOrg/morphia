package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.Expressions;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class DateToString extends Expression {
    private Expression format;
    private Expression date;
    private Expression timeZone;
    private Expression onNull;

    public DateToString() {
        super("$dateToString");
    }

    public DateToString date(String date) {
        return date(Expressions.value(date));
    }

    public DateToString date(Expression date) {
        this.date = date;
        return this;
    }

    public Expression format() {
        return format;
    }

    public Expression date() {
        return date;
    }

    public Expression timeZone() {
        return timeZone;
    }

    public Expression onNull() {
        return onNull;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    public DateToString format(String format) {
        return format(Expressions.value(format));
    }

    public DateToString format(Expression format) {
        this.format = format;
        return this;
    }

    public DateToString onNull(String onNull) {
        return onNull(Expressions.value(onNull));
    }

    public DateToString onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }

    public DateToString timeZone(String timeZone) {
        return timeZone(Expressions.value(timeZone));
    }

    public DateToString timeZone(Expression timeZone) {
        this.timeZone = timeZone;
        return this;
    }
}
