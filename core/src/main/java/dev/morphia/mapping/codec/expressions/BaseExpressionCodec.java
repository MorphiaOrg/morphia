package dev.morphia.mapping.codec.expressions;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.SingleValuedExpression;
import dev.morphia.annotations.internal.MorphiaInternal;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public abstract class BaseExpressionCodec<T> implements Codec<T> {

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @MorphiaInternal
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void encodeIfNotNull(CodecRegistry registry, BsonWriter writer, @Nullable Expression expression, EncoderContext encoderContext) {
        if (expression != null) {
            Codec codec = registry.get(expression.getClass());
            if (expression instanceof SingleValuedExpression) {
                codec.encode(writer, expression, encoderContext);
            } else {
                document(writer, () -> {
                    codec.encode(writer, expression, encoderContext);
                });
            }
        }
    }

    @MorphiaInternal
    void encodeIfNotNull(CodecRegistry registry, BsonWriter writer, String name, @Nullable Expression expression,
                                  EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            encodeIfNotNull(registry, writer, expression, encoderContext);
        }
    }


    void document(BsonWriter writer, Runnable body) {
        writer.writeStartDocument();
        body.run();
        writer.writeEndDocument();
    }

    void array(BsonWriter writer, String name, Runnable body) {
        writer.writeStartArray(name);
        body.run();
        writer.writeEndArray();
    }

}
