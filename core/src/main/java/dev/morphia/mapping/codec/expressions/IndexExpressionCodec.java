package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.IndexExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.array;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class IndexExpressionCodec extends BaseExpressionCodec<IndexExpression> {
    public IndexExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, IndexExpression index, EncoderContext encoderContext) {
        array(writer, index.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, index.string(), encoderContext);
            encodeIfNotNull(registry, writer, index.substring(), encoderContext);
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
    public Class<IndexExpression> getEncoderClass() {
        return IndexExpression.class;
    }
}
