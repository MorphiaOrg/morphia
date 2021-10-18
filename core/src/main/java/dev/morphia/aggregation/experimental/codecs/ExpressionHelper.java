package dev.morphia.aggregation.experimental.codecs;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.function.Consumer;

/**
 * @morphia.internal
 * @since 2.1
 */
public final class ExpressionHelper {
    private ExpressionHelper() {
    }

    public static void array(BsonWriter writer, Runnable body) {
        writer.writeStartArray();
        body.run();
        writer.writeEndArray();
    }

    public static void array(BsonWriter writer, String name, Runnable body) {
        writer.writeStartArray(name);
        body.run();
        writer.writeEndArray();
    }

    public static void document(BsonWriter writer, Runnable body) {
        writer.writeStartDocument();
        body.run();
        writer.writeEndDocument();
    }

    public static void document(BsonWriter writer, String name, Runnable body) {
        writer.writeStartDocument(name);
        body.run();
        writer.writeEndDocument();
    }

    public static Document document(Mapper mapper, Document seed, Consumer<BsonWriter> body) {
        DocumentWriter writer = new DocumentWriter(mapper, seed);
        writer.writeStartDocument();
        body.accept(writer);
        writer.writeEndDocument();

        return writer.getDocument();
    }

    /**
     * @param datastore
     * @param writer
     * @param name
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    public static void expression(Datastore datastore, BsonWriter writer, String name, @Nullable Expression expression,
                                  EncoderContext encoderContext) {
        if (expression != null) {
            writer.writeName(name);
            expression.encode(datastore, writer, encoderContext);
        }
    }

    /**
     * @param datastore
     * @param writer
     * @param expression
     * @param encoderContext
     * @morphia.internal
     */
    public static void expression(Datastore datastore, BsonWriter writer, @Nullable Expression expression, EncoderContext encoderContext) {
        if (expression != null) {
            expression.encode(datastore, writer, encoderContext);
        }
    }

    /**
     * @param datastore
     * @param writer
     * @param name
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    public static void value(Datastore datastore, BsonWriter writer, String name, @Nullable Object value, EncoderContext encoderContext) {
        if (value != null) {
            writer.writeName(name);
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }

    /**
     * @param datastore
     * @param writer
     * @param value
     * @param encoderContext
     * @morphia.internal
     */
    public static void value(Datastore datastore, BsonWriter writer, @Nullable Object value, EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }
}
