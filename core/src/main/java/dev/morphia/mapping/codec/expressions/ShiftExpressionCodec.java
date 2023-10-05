package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.ShiftExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class ShiftExpressionCodec extends BaseExpressionCodec<ShiftExpression> {
    public ShiftExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ShiftExpression shift, EncoderContext encoderContext) {
        document(writer, shift.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "output", shift.output(), encoderContext);
            writer.writeInt64("by", shift.by());
            encodeIfNotNull(registry, writer, "default", shift.defaultValue(), encoderContext);
        });

    }

    @Override
    public Class<ShiftExpression> getEncoderClass() {
        return ShiftExpression.class;
    }
}
