package dev.morphia.mapping.codec.expressions;

import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.SingleValuedExpression;
import dev.morphia.annotations.internal.MorphiaInternal;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class ExpressionCodecHelper {
    static void array(BsonWriter writer, String name, Runnable body) {
        writer.writeStartArray(name);
        body.run();
        writer.writeEndArray();
    }

    static void array(CodecRegistry codecRegistry, BsonWriter writer, String name, @Nullable List<Expression> list,
                      EncoderContext encoderContext) {
        if (list != null) {
            array(writer, name, () -> {
                for (Expression expression : list) {
                    encodeIfNotNull(codecRegistry, writer, expression, encoderContext);
                }
            });
        }
    }

    static void document(BsonWriter writer, String name, Runnable body) {
        writer.writeStartDocument(name);
        body.run();
        writer.writeEndDocument();
    }

    static void document(BsonWriter writer, Runnable body) {
        writer.writeStartDocument();
        body.run();
        writer.writeEndDocument();
    }

    @MorphiaInternal
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean encodeIfNotNull(CodecRegistry registry,
                                          BsonWriter writer,
                                          @Nullable Expression expression,
                                          EncoderContext encoderContext) {
        if (expression != null) {
            Codec codec = registry.get(expression.getClass());
            if (expression instanceof SingleValuedExpression) {
                codec.encode(writer, expression, encoderContext);
            } else {
                document(writer, () -> {
                    codec.encode(writer, expression, encoderContext);
                });
            }
            return true;
        }
        return false;
    }

    @MorphiaInternal
    public static boolean encodeIfNotNull(CodecRegistry registry, BsonWriter writer, String name, @Nullable Expression expression,
                                          EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            encodeIfNotNull(registry, writer, expression, encoderContext);
            return true;
        }
        return false;
    }
}
