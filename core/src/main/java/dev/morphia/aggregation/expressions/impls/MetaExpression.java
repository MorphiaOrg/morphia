package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class MetaExpression extends Expression {

    public MetaExpression() {
        super("$meta");
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        ExpressionHelper.expression(datastore, writer, getOperation(), new ValueExpression("textScore"), encoderContext);
    }
}
