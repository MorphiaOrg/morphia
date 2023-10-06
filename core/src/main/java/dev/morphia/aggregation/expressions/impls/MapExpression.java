package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Applies a subexpression to each element of an array and returns the array of resulting values in order. Accepts named parameters.
 */
public class MapExpression extends Expression {
    private final Expression input;
    private final Expression in;
    private String as;

    /**
     * @param input the input expression
     * @param in    the in expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MapExpression(Expression input, Expression in) {
        super("$map");
        this.input = input;
        this.in = in;
    }

    /**
     * @param as the 'as' value
     * @return this
     */
    public MapExpression as(String as) {
        this.as = as;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the input
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the in expression
     */
    @MorphiaInternal
    public Expression in() {
        return in;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the as value
     */
    @MorphiaInternal
    @Nullable
    public String as() {
        return as;
    }
}
