package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines expressions for $replaceAll and $replaceOne
 *
 * @since 2.1
 */
public class ReplaceExpression extends Expression {
    private final Expression find;
    private final Expression replacement;
    private final Expression input;

    /**
     * @param operator    the operator name
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @morphia.internal
     */
    @MorphiaInternal
    public ReplaceExpression(String operator,
            Expression input,
            Expression find,
            Expression replacement) {
        super(operator);
        this.input = input;
        this.find = find;
        this.replacement = replacement;
    }

    public Expression find() {
        return find;
    }

    public Expression replacement() {
        return replacement;
    }

    public Expression input() {
        return input;
    }
}
