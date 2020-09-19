package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class ReduceExpression extends Expression {
    private final Expression input;
    private final Expression initial;
    private final Expression in;

    public ReduceExpression(Expression input, Expression initial, Expression in) {
        super("$reduce");
        this.input = input;
        this.initial = initial;
        this.in = in;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        ExpressionCodec.writeNamedExpression(mapper, writer, "input", input, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "initialValue", initial, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "in", in, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
