package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class LetExpression extends Expression {
    private final Expression in;
    private DocumentExpression documentExpression = Expressions.of();

    public LetExpression(final Expression in) {
        super("$let");
        this.in = in;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        writer.writeName("vars");
        documentExpression.encode(mapper, writer, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "in", in, encoderContext);
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
    public LetExpression variable(final String name, final Expression expression) {
        documentExpression.field(name, expression);
        return this;
    }
}
