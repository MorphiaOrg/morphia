package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

/**
 * Base class for the math expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#arithmetic-expression-operators Arithmetic Expressions
 * @since 2.0
 */
public class MathExpression extends Expression {

    /**
     * @param operation
     * @param operands
     * @morphia.internal
     */
    @MorphiaInternal
    public MathExpression(String operation, List<Expression> operands) {
        super(operation, new ExpressionList(operands));
    }

    /**
     * @param operation
     * @param operand
     * @morphia.internal
     */
    @MorphiaInternal
    public MathExpression(String operation, Expression operand) {
        super(operation, new ExpressionList(operand));
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        ExpressionList value = value();
        if (value != null) {
            final List<Expression> operands = value.getValues();
            writer.writeName(operation());
            if (operands.size() > 1) {
                writer.writeStartArray();
            }
            for (Expression operand : operands) {
                if (operand != null) {
                    wrapExpression(datastore, writer, operand, encoderContext);
                } else {
                    writer.writeNull();
                }
            }
            if (operands.size() > 1) {
                writer.writeEndArray();
            }
        }
    }

    @Override
    public ExpressionList value() {
        return (ExpressionList) super.value();
    }
}
