package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

public class TrimExpression extends Expression {
    private final Expression input;
    private Expression chars;

    public TrimExpression(String operator, Expression input) {
        super(operator);
        this.input = input;
    }

    public TrimExpression chars(Expression chars) {
        this.chars = chars;
        return this;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                expression(mapper, writer, "input", input, encoderContext);
                expression(mapper, writer, "chars", chars, encoderContext);
            });
        });
    }
}
