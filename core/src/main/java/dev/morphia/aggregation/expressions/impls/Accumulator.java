package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * Base class for the accumulator expression types.
 *
 * @since 2.0
 */
public class Accumulator extends Expression {

    /**
     * @param operation
     * @param values
     * @morphia.internal
     */
    @MorphiaInternal
    public Accumulator(String operation, List<Expression> values) {
        super(operation, new ExpressionList(values));
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExpressionList value() {
        return (ExpressionList) super.value();
    }
}
