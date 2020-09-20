package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedExpression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedValue;

public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private String as;

    public ArrayFilterExpression(Expression array, Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    public ArrayFilterExpression as(String as) {
        this.as = as;
        return this;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        writeNamedExpression(mapper, writer, "input", array, encoderContext);
        writeNamedExpression(mapper, writer, "cond", conditional, encoderContext);
        writeNamedValue(mapper, writer, "as", as, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
