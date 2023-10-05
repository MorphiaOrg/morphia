package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.SetFieldExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class SetFieldExpressionCodec extends BaseExpressionCodec<SetFieldExpression> {
    public SetFieldExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SetFieldExpression set, EncoderContext encoderContext) {
        document(writer, set.operation(), () -> {
            CodecRegistry registry = datastore.getCodecRegistry();
            encodeIfNotNull(registry, writer, "field", set.field(), encoderContext);
            value(registry, writer, "input", set.input(), encoderContext);
            encodeIfNotNull(registry, writer, "value", set.value(), encoderContext);
        });

    }

    @Override
    public Class<SetFieldExpression> getEncoderClass() {
        return SetFieldExpression.class;
    }
}
