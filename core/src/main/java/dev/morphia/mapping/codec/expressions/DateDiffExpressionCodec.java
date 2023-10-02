package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DateDiffExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Locale;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.value;

public class DateDiffExpressionCodec extends BaseExpressionCodec<DateDiffExpression> {
    public DateDiffExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateDiffExpression diff, EncoderContext encoderContext) {
        document(writer, diff.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "startDate", diff.startDate(), encoderContext);
            encodeIfNotNull(registry, writer, "endDate", diff.endDate(), encoderContext);
            value(writer, "unit", diff.unit());
            encodeIfNotNull(registry, writer, "timezone", diff.timezone(), encoderContext);
            value(writer, "startOfWeek", diff.startOfWeek());
        });

    }

    @Override
    public Class<DateDiffExpression> getEncoderClass() {
        return DateDiffExpression.class;
    }
}
