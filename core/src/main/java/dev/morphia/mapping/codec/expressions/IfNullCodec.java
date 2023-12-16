package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.IfNull;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class IfNullCodec extends BaseExpressionCodec<IfNull> {
    private final MorphiaDatastore datastore;

    public IfNullCodec(MorphiaDatastore datastore) {
        super(datastore);
        this.datastore = datastore;
    }

    @Override
    public void encode(BsonWriter writer, IfNull value, EncoderContext encoderContext) {
        array(writer, value.operation(), () -> {
            CodecRegistry codecRegistry = datastore.getCodecRegistry();
            value.input().forEach(i -> {
                encodeIfNotNull(codecRegistry, writer, i, encoderContext);
            });
            encodeIfNotNull(codecRegistry, writer, value.replacement(), encoderContext);
            encodeIfNotNull(codecRegistry, writer, value.document(), encoderContext);
        });

    }

    @Override
    public Class<IfNull> getEncoderClass() {
        return IfNull.class;
    }
}
