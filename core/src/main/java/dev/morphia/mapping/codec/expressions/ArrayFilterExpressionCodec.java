package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ArrayFilterExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class ArrayFilterExpressionCodec extends BaseExpressionCodec<ArrayFilterExpression> {
    public ArrayFilterExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ArrayFilterExpression filter, EncoderContext encoderContext) {
        document(writer, filter.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "input", filter.array(), encoderContext);
            encodeIfNotNull(registry, writer, "cond", filter.conditional(), encoderContext);
            encodeIfNotNull(registry, writer, "as", filter.as(), encoderContext);
        });

    }

    @Override
    public Class<ArrayFilterExpression> getEncoderClass() {
        return ArrayFilterExpression.class;
    }
}
