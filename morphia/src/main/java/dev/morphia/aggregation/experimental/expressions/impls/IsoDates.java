package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Reusable type for ISO Date related expressions.
 *
 * @morphia.internal
 * @see dev.morphia.aggregation.experimental.expressions.DateExpressions#isoDayOfWeek(Expression)
 * @see dev.morphia.aggregation.experimental.expressions.DateExpressions#isoWeek(Expression)
 * @see dev.morphia.aggregation.experimental.expressions.DateExpressions#isoWeekYear(Expression)
 */
public class IsoDates extends Expression {
    private Expression date;
    private Expression timezone;

    public IsoDates(final String operation, final Expression date) {
        super(operation);
        this.date = date;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());

        writer.writeStartDocument();
        ExpressionCodec.writeNamedExpression(mapper, writer, "date", date, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "timezone", timezone, encoderContext);
        writer.writeEndDocument();

        writer.writeEndDocument();
    }

    /**
     * The optional timezone to use to format the date. By default, it uses UTC.
     *
     * @param timezone the expression
     * @return this
     */
    public IsoDates timezone(final Expression timezone) {
        this.timezone = timezone;
        return this;
    }
}
