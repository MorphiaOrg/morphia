package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

public abstract class MathExpression extends Expression {
    protected final List<Expression> operands;

    protected MathExpression(final String operation, final List<Expression> operands) {
        super(operation);
        this.operands = operands;
    }

    /**
     * Returns the result of dividing the first number by the second. Accepts two argument expressions.
     *
     * @return the new expression
     *
     * @mongodb.driver.manual reference/operator/aggregation/divide $divide
     */
    public static MathExpression divide(final Expression numerator, final Expression divisor) {
        return new MathExpression("$divide", List.of(numerator, divisor)) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
                writer.writeStartDocument();
                writer.writeStartArray(getOperation());
                for (final Expression operand : operands) {
                    writeUnnamedExpression(mapper, writer, operand, encoderContext);
                }
                writer.writeEndArray();
                writer.writeEndDocument();
            }
        };
    }
}
