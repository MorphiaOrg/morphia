package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.IfNull;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class IfNullCodec extends BaseExpressionCodec<IfNull> {
    private final MorphiaDatastore datastore;

    public IfNullCodec(MorphiaDatastore datastore) {
        super(datastore);
        this.datastore = datastore;
    }

    @Override
    public void encode(BsonWriter writer, IfNull value, EncoderContext encoderContext) {
        ExpressionCodecHelper.array(writer, value.operation(), () -> {
            CodecRegistry codecRegistry = datastore.getCodecRegistry();
            ExpressionCodecHelper.encodeIfNotNull(codecRegistry, writer, value.target(), encoderContext);
            ExpressionCodecHelper.encodeIfNotNull(codecRegistry, writer, value.replacement(), encoderContext);
            ExpressionCodecHelper.encodeIfNotNull(codecRegistry, writer, value.document(), encoderContext);
        });

    }

    @Override
    public Class<IfNull> getEncoderClass() {
        return IfNull.class;
    }
}
