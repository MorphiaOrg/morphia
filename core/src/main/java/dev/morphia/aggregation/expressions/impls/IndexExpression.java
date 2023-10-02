package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

/**
 * Defines the $indexOfBytes expression
 *
 * @since 2.0
 */
public class IndexExpression extends Expression {
    private final Expression string;
    private final Expression substring;
    private Integer end;
    private Integer start;

    /**
     * Creates the new expression
     *
     * @param operation the index operation name
     * @param string    the string to search
     * @param substring the target string
     * @morphia.internal
     */
    @MorphiaInternal
    public IndexExpression(String operation, Expression string, Expression substring) {
        super(operation);
        this.string = string;
        this.substring = substring;
    }

    public Expression string() {
        return string;
    }

    public Expression substring() {
        return substring;
    }

    @Nullable
    public Integer end() {
        return end;
    }

    @Nullable
    public Integer start() {
        return start;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the end boundary for searching
     *
     * @param end the end
     * @return this
     */
    public IndexExpression end(int end) {
        this.end = end;
        return this;
    }

    /**
     * Sets the start boundary for searching
     *
     * @param start the start
     * @return this
     */
    public IndexExpression start(int start) {
        this.start = start;
        return this;
    }
}
