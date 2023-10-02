package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DateFromString;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class DateFromStringCodec extends BaseExpressionCodec<DateFromString> {
    public DateFromStringCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateFromString date, EncoderContext encoderContext) {
        document(writer, date.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "dateString", date.dateString(), encoderContext);
            encodeIfNotNull(registry, writer, "format", date.format(), encoderContext);
            encodeIfNotNull(registry, writer, "timezone", date.timeZone(), encoderContext);
            encodeIfNotNull(registry, writer, "onError", date.onError(), encoderContext);
            encodeIfNotNull(registry, writer, "onNull", date.onNull(), encoderContext);
        });

    }

    @Override
    public Class<DateFromString> getEncoderClass() {
        return DateFromString.class;
    }
}
