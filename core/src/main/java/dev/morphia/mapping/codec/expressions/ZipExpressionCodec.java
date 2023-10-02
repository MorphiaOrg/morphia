package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ZipExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.array;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class ZipExpressionCodec extends BaseExpressionCodec<ZipExpression> {
    public ZipExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ZipExpression zip, EncoderContext encoderContext) {
        document(writer, zip.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            array(registry, writer, "inputs", zip.inputs(), encoderContext);
            encodeIfNotNull(registry, writer, "useLongestLength", zip.useLongestLength(), encoderContext);
            encodeIfNotNull(registry, writer, "defaults", zip.defaults(), encoderContext);
        });

    }

    @Override
    public Class<ZipExpression> getEncoderClass() {
        return ZipExpression.class;
    }
}
