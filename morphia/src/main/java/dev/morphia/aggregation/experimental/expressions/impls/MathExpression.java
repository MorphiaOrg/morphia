package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static java.util.Arrays.asList;

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
    public MathExpression(String operation, List<Expression> operands) {
        super(operation);
        this.operands = operands;
    }

    /**
     * @param operation
     * @param operand
     * @morphia.internal
     */
    public MathExpression(String operation, Expression operand) {
        super(operation);
        this.operands = asList(operand);
    }

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        if (operands.size() > 1) {
            writer.writeStartArray();
        }
        for (Expression operand : operands) {
            if (operand != null) {
                expression(mapper, writer, operand, encoderContext);
            } else {
                writer.writeNull();
            }
        }
        if (operands.size() > 1) {
            writer.writeEndArray();
        }
        writer.writeEndDocument();
    }
}
