package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.MetaExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class MetaExpressionCodec extends BaseExpressionCodec<MetaExpression> {
    public MetaExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, MetaExpression value, EncoderContext encoderContext) {
        writer.writeName(value.operation());
        writer.writeString("textScore");
    }

    @Override
    public Class<MetaExpression> getEncoderClass() {
        return MetaExpression.class;
    }
}
