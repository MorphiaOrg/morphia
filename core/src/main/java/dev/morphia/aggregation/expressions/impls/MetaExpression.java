package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class MetaExpression extends Expression {

    public MetaExpression() {
        super("$meta");
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        ExpressionHelper.expression(datastore, writer, getOperation(), new ValueExpression("textScore"), encoderContext);
    }
}
