package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class ExpressionListCodec extends BaseExpressionCodec<ExpressionList> {
    public ExpressionListCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ExpressionList list, EncoderContext encoderContext) {
        array(writer, () -> {
            for (Expression expression : list.getValues()) {
                encodeIfNotNull(datastore.getCodecRegistry(), writer, expression, encoderContext);
            }
        });
    }

    @Override
    public Class<ExpressionList> getEncoderClass() {
        return ExpressionList.class;
    }
}
