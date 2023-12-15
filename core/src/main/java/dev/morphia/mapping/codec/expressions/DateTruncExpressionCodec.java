package dev.morphia.mapping.codec.expressions;

import java.util.Locale;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DateTruncExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class DateTruncExpressionCodec extends BaseExpressionCodec<DateTruncExpression> {
    public DateTruncExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateTruncExpression trunc, EncoderContext encoderContext) {
        document(writer, trunc.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "date", trunc.date(), encoderContext);
            writer.writeString("unit", trunc.unit().name().toLowerCase(Locale.ROOT));
            value(writer, "binSize", trunc.binSize());
            encodeIfNotNull(registry, writer, "timezone", trunc.timezone(), encoderContext);
            if (trunc.startOfWeek() != null) {
                value(writer, "startOfWeek", trunc.startOfWeek().name().toLowerCase(Locale.ROOT));
            }
        });

    }

    @Override
    public Class<DateTruncExpression> getEncoderClass() {
        return DateTruncExpression.class;
    }
}
