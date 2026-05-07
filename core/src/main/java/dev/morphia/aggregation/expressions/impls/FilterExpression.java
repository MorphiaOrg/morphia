package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

public class FilterExpression extends Expression {
    private final Expression input;
    private final Expression cond;
    private String as;
    private Expression limit;

    /**
     * @param input the input array expression
     * @param cond  the condition expression
     * @hidden
     */
    @MorphiaInternal
    public FilterExpression(Expression input, Expression cond) {
        super("$filter");
        this.input = input;
        this.cond = cond;
    }

    /**
     * @hidden
     * @return the input
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @return the conditional
     */
    @MorphiaInternal
    public Expression cond() {
        return cond;
    }

    /**
     * @hidden
     * @return the as expression
     */
    @MorphiaInternal
    public String as() {
        return as;
    }

    /**
     * @param as the variable name for the element alias
     * @return this
     */
    public FilterExpression as(String as) {
        this.as = as;
        return this;
    }

    /**
     * @hidden
     * @return the limit expression
     */
    @MorphiaInternal
    public Expression limit() {
        return limit;
    }

    /**
     * @param limit the maximum number of elements to include
     * @return this
     */
    public FilterExpression limit(Object limit) {
        this.limit = Expressions.wrap(limit);
        return this;
    }
}
