package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

/**
 * Base class for the math expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#arithmetic-expression-operators Arithmetic Expressions
 * @since 2.0
 */
public class MathExpression extends Expression {
    private final List<Expression> operands;

    /**
     * @param operation
     * @param operands
     * @morphia.internal
     */
    public MathExpression(final String operation, final List<Expression> operands) {
        super(operation);
        this.operands = operands;
    }

    /**
     * @param operation
     * @param operand
     * @morphia.internal
     */
    public MathExpression(final String operation, final Expression operand) {
        super(operation);
        this.operands = List.of(operand);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        if (operands.size() > 1) {
            writer.writeStartArray();
        }
        for (final Expression operand : operands) {
            ExpressionCodec.writeUnnamedExpression(mapper, writer, operand, encoderContext);
        }
        if (operands.size() > 1) {
            writer.writeEndArray();
        }
        writer.writeEndDocument();
    }
}
