package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ArrayIndexExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class ArrayIndexExpressionCodec extends BaseExpressionCodec<ArrayIndexExpression> {
    public ArrayIndexExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ArrayIndexExpression index, EncoderContext encoderContext) {
        array(writer, index.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, index.array(), encoderContext);
            encodeIfNotNull(registry, writer, index.search(), encoderContext);
            Integer start = index.start();
            if (start != null) {
                writer.writeInt32(start);
            }
            Integer end = index.end();
            if (end != null) {
                writer.writeInt32(end);
            }
        });

    }

    @Override
    public Class<ArrayIndexExpression> getEncoderClass() {
        return ArrayIndexExpression.class;
    }
}
