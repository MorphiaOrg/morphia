package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class MapExpression extends Expression {
    private final Expression input;
    private final Expression in;
    private String as;

    public MapExpression(final Expression input, final Expression in) {
        super("$map");
        this.input = input;
        this.in = in;
    }

    public MapExpression as(final String as) {
        this.as = as;
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        ExpressionCodec.writeNamedExpression(mapper, writer, "input", input, encoderContext);
        ExpressionCodec.writeNamedExpression(mapper, writer, "in", in, encoderContext);
        ExpressionCodec.writeNamedValue(mapper, writer, "as", as, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }
}
