package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

public class SetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;
    private final Expression value;

    public SetFieldExpression(Expression field, Object input, Expression value) {
        super("$setField");
        this.field = field;
        this.input = input;
        this.value = value;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "field", field, encoderContext);
            ExpressionHelper.value(datastore, writer, "input", input, encoderContext);
            expression(datastore, writer, "value", value, encoderContext);
        });
    }
}
