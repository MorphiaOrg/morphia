package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.Push;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PushCodec extends BaseExpressionCodec<Push> {
    public PushCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, Push push, EncoderContext encoderContext) {
        writer.writeName(push.operation());
        CodecRegistry registry = datastore.getCodecRegistry();
        if (push.field() != null) {
            Codec codec = registry.get(push.field().getClass());
            codec.encode(writer, push.field(), encoderContext);
        } else if (push.document() != null) {
            Codec codec = registry.get(push.document().getClass());
            codec.encode(writer, push.document(), encoderContext);
        }

    }

    @Override
    public Class<Push> getEncoderClass() {
        return Push.class;
    }
}
