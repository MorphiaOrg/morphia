package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.IsoDates;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class IsoDatesCodec extends BaseExpressionCodec<IsoDates> {
    public IsoDatesCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, IsoDates dates, EncoderContext encoderContext) {
        document(writer, dates.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "date", dates.date(), encoderContext);
            encodeIfNotNull(registry, writer, "timezone", dates.timezone(), encoderContext);
        });

    }

    @Override
    public Class<IsoDates> getEncoderClass() {
        return IsoDates.class;
    }
}
