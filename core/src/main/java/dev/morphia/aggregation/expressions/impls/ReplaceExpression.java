package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines expressions for $replaceAll and $replaceOne
 *
 * @since 2.1
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ReplaceExpression extends Expression {
    private final Expression find;
    private final Expression replacement;
    private final Expression input;

    /**
     * @param operator    the operator name
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @hidden
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

    /**
     * @hidden
     * @morphia.internal
     * @return the find
     */
    @MorphiaInternal
    public Expression find() {
        return find;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the replacement
     */
    @MorphiaInternal
    public Expression replacement() {
        return replacement;
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
}
