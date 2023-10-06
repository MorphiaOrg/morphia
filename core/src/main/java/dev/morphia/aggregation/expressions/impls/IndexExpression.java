package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public IndexExpression(String operation, Expression string, Expression substring) {
        super(operation);
        this.string = string;
        this.substring = substring;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the string
     */
    @MorphiaInternal
    public Expression string() {
        return string;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the substring
     */
    @MorphiaInternal
    public Expression substring() {
        return substring;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the end
     */
    @MorphiaInternal
    @Nullable
    public Integer end() {
        return end;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the start
     */
    @MorphiaInternal
    @Nullable
    public Integer start() {
        return start;
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
