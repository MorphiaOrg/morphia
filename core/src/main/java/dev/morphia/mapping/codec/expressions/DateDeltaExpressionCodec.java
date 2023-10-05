package dev.morphia.mapping.codec.expressions;

import java.util.Locale;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DateDeltaExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class DateDeltaExpressionCodec extends BaseExpressionCodec<DateDeltaExpression> {
    public DateDeltaExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, DateDeltaExpression delta, EncoderContext encoderContext) {
        document(writer, delta.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "startDate", delta.startDate(), encoderContext);
            writer.writeString("unit", delta.unit().name().toLowerCase(Locale.ROOT));
            writer.writeInt64("amount", delta.amount());
            encodeIfNotNull(registry, writer, "timezone", delta.timezone(), encoderContext);
        });

    }

    @Override
    public Class<DateDeltaExpression> getEncoderClass() {
        return DateDeltaExpression.class;
    }
}
