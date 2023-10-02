package dev.morphia.aggregation.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Base class for the date expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#date-expression-operators Date Expressions
 */
public class DateExpression extends Expression {
    protected DateExpression(String operation, Expression value) {
        super(operation, value);
    }

    @Override
    @MorphiaInternal
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        ExpressionHelper.expression(datastore, writer, operation(), value(), encoderContext);
    }
}
