package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class MapExpression extends Expression {
    private final Expression input;
    private final Expression in;
    private String as;

    public MapExpression(Expression input, Expression in) {
        super("$map");
        this.input = input;
        this.in = in;
    }

    public MapExpression as(String as) {
        this.as = as;
        return this;
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                expression(mapper, writer, "input", input, encoderContext);
                expression(mapper, writer, "in", in, encoderContext);
                value(mapper, writer, "as", as, encoderContext);
            });
        });
    }
}
