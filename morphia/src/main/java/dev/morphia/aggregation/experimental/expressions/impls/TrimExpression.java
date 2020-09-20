package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedExpression;

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
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        writeNamedExpression(mapper, writer, "input", input, encoderContext);
        writeNamedExpression(mapper, writer, "chars", chars, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
