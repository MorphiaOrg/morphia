package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

public class MapExpression extends Expression {
    private final Expression input;
    private final Expression in;
    private String as;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MapExpression(Expression input, Expression in) {
        super("$map");
        this.input = input;
        this.in = in;
    }

    public MapExpression as(String as) {
        this.as = as;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression in() {
        return in;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public String as() {
        return as;
    }
}
