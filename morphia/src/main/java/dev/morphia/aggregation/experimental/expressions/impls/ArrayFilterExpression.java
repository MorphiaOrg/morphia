package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private String as;

    public ArrayFilterExpression(final Expression array, final Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    public ArrayFilterExpression as(final String as) {
        this.as = as;
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        ExpressionCodec.writeNamedExpression(mapper, writer, "input", array, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "cond", conditional, encoderContext);
        ExpressionCodec.writeNamedValue(mapper, writer, "as", as, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
