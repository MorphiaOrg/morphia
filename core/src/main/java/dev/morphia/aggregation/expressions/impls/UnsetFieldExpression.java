package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

public class UnsetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;

    public UnsetFieldExpression(Expression field, Object input) {
        super("$unsetField");
        this.field = field;
        this.input = input;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "field", field, encoderContext);
            value(datastore, writer, "input", input, encoderContext);
        });
    }
}
