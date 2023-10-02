package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DateToString;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class DateToStringCodec extends BaseExpressionCodec<DateToString> {
    public DateToStringCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateToString value, EncoderContext encoderContext) {
        document(writer, value.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "date", value.date(), encoderContext);
            encodeIfNotNull(registry, writer, "format", value.format(), encoderContext);
            encodeIfNotNull(registry, writer, "timezone", value.timeZone(), encoderContext);
            encodeIfNotNull(registry, writer, "onNull", value.onNull(), encoderContext);
        });

    }

    @Override
    public Class<DateToString> getEncoderClass() {
        return DateToString.class;
    }
}
