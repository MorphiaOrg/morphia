package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.GetFieldExpression;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class GetFieldExpressionCodec extends BaseExpressionCodec<GetFieldExpression> {
    public GetFieldExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, GetFieldExpression getField, EncoderContext encoderContext) {
        writer.writeName(getField.operation());
        if (getField.input() == null) {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, getField.value(), encoderContext);
        } else {
            document(writer, () -> {
                encodeIfNotNull(datastore.getCodecRegistry(), writer, "field", getField.value(), encoderContext);
                encodeIfNotNull(datastore.getCodecRegistry(), writer, "input", getField.input(), encoderContext);
            });
        }
    }

    @Override
    public Class<GetFieldExpression> getEncoderClass() {
        return GetFieldExpression.class;
    }
}
