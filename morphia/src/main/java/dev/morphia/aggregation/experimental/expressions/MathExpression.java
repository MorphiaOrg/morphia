package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static java.util.Arrays.asList;

public class MathExpression extends Expression {
    protected final List<Expression> operands;

    protected MathExpression(final String operation, final List<Expression> operands) {
        super(operation);
        this.operands = operands;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartArray(getOperation());
        for (final Expression operand : operands) {
            ExpressionCodec.writeUnnamedExpression(mapper, writer, operand, encoderContext);
        }
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    /**
     * Returns the result of dividing the first number by the second. Accepts two argument expressions.
     *
     * @return the new expression
     *
     * @mongodb.driver.manual reference/operator/aggregation/divide $divide
     */
    public static MathExpression divide(final Expression numerator, final Expression divisor) {
        return new MathExpression("$divide", List.of(numerator, divisor));
    }

    /**
     * Multiplies numbers together and returns the result. Pass the arguments to $multiply in an array.
     *
     * @return the new expression
     *
     * @mongodb.driver.manual reference/operator/aggregation/multiply $multiply
     */
    public static Expression multiply(final Expression... values) {
        return new MathExpression("$multiply", asList(values));
    }
}
