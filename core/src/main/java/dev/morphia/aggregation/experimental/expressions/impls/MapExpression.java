package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.wrapExpression;

public class MapExpression extends Expression {
    private final Expression input;
    private final Expression in;
    private String as;

    public MapExpression(Expression input, Expression in) {
        super("$map");
        this.input = input;
        this.in = in;
    }

    public MapExpression as(String as) {
        this.as = as;
        return this;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            wrapExpression(datastore, writer, "input", input, encoderContext);
            wrapExpression(datastore, writer, "in", in, encoderContext);
            value(datastore, writer, "as", as, encoderContext);
        });
    }
}
