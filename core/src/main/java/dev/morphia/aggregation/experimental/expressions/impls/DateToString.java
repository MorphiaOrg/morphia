package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

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

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                expression(datastore, writer, "date", date, encoderContext);
                expression(datastore, writer, "format", format, encoderContext);
                expression(datastore, writer, "timezone", timeZone, encoderContext);
                expression(datastore, writer, "onNull", onNull, encoderContext);
            });
        });
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
