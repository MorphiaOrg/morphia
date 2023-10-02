package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DateFromParts;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class DateFromPartsCodec extends BaseExpressionCodec<DateFromParts> {
    public DateFromPartsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateFromParts parts, EncoderContext encoderContext) {
        document(writer, parts.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "year", parts.year(), encoderContext);
            encodeIfNotNull(registry, writer, "month", parts.month(), encoderContext);
            encodeIfNotNull(registry, writer, "day", parts.day(), encoderContext);
            encodeIfNotNull(registry, writer, "hour", parts.hour(), encoderContext);
            encodeIfNotNull(registry, writer, "minute", parts.minute(), encoderContext);
            encodeIfNotNull(registry, writer, "second", parts.second(), encoderContext);
            encodeIfNotNull(registry, writer, "millisecond", parts.millisecond(), encoderContext);
            encodeIfNotNull(registry, writer, "isoWeekYear", parts.isoWeekYear(), encoderContext);
            encodeIfNotNull(registry, writer, "isoWeek", parts.isoWeek(), encoderContext);
            encodeIfNotNull(registry, writer, "isoDayOfWeek", parts.isoDayOfWeek(), encoderContext);
            encodeIfNotNull(registry, writer, "timezone", parts.timezone(), encoderContext);
        });

    }

    @Override
    public Class<DateFromParts> getEncoderClass() {
        return DateFromParts.class;
    }
}
