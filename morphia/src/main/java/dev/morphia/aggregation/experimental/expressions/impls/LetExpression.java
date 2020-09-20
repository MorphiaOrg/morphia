package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.writeNamedExpression;

public class LetExpression extends Expression {
    private final Expression in;
    private final DocumentExpression variables = Expressions.of();

    public LetExpression(Expression in) {
        super("$let");
        this.in = in;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        writer.writeName("vars");
        variables.encode(mapper, writer, encoderContext);
        writeNamedExpression(mapper, writer, "in", in, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    /**
     * Defines a new variable
     *
     * @param name       the variable name
     * @param expression the value expression
     * @return this
     */
    public LetExpression variable(String name, Expression expression) {
        variables.field(name, expression);
        return this;
    }
}
