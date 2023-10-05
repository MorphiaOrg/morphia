package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ArrayLiteral;
import dev.morphia.aggregation.expressions.impls.Expression;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ArrayLiteralCodec extends BaseExpressionCodec<ArrayLiteral> {
    public ArrayLiteralCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ArrayLiteral literal, EncoderContext encoderContext) {
        Object[] objects = literal.objects();
        CodecRegistry codecRegistry = datastore.getCodecRegistry();
        if (objects == null) {
            Expression value = literal.value();
            if (value != null) {
                Codec codec = codecRegistry.get(value.getClass());
                codec.encode(writer, value, encoderContext);
            }
        } else {
            array(writer, () -> {
                for (Object object : objects) {
                    Codec codec = datastore.getCodecRegistry().get(object.getClass());
                    encoderContext.encodeWithChildContext(codec, writer, object);
                }
            });
        }

    }

    @Override
    public Class<ArrayLiteral> getEncoderClass() {
        return ArrayLiteral.class;
    }
}
