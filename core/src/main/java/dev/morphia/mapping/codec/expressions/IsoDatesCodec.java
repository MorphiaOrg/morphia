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
        CodecRegistry registry = datastore.getCodecRegistry();
        if (dates.timezone() == null) {
            encodeIfNotNull(registry, writer, dates.operation(), dates.date(), encoderContext);
        } else {
            document(writer, dates.operation(), () -> {
                encodeIfNotNull(registry, writer, "date", dates.date(), encoderContext);
                encodeIfNotNull(registry, writer, "timezone", dates.timezone(), encoderContext);
            });
        }

    }

    @Override
    public Class<IsoDates> getEncoderClass() {
        return IsoDates.class;
    }
}
