package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

/**
 * @since 2.0
 */
public class ArrayIndexExpression extends Expression {
    private final Expression array;
    private final Expression search;
    private Integer start;
    private Integer end;

    /**
     * @param array
     * @param search
     * @morphia.internal
     */
    @MorphiaInternal
    public ArrayIndexExpression(Expression array, Expression search) {
        super("$indexOfArray");
        this.array = array;
        this.search = search;
    }

    public Expression array() {
        return array;
    }

    public Expression search() {
        return search;
    }

    @Nullable
    public Integer start() {
        return start;
    }

    @Nullable
    public Integer end() {
        return end;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    /**
     * The ending index
     *
     * @param end the ending index
     * @return this
     */
    public ArrayIndexExpression end(Integer end) {
        this.end = end;
        return this;
    }

    /**
     * The starting index
     *
     * @param start the starting index
     * @return this
     */
    public ArrayIndexExpression start(Integer start) {
        this.start = start;
        return this;
    }
}
