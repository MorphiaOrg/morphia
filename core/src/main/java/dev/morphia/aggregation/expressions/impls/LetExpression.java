package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

public class LetExpression extends Expression {
    private final Expression in;
    private final DocumentExpression variables = Expressions.of();

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public LetExpression(Expression in) {
        super("$let");
        this.in = in;
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
    public DocumentExpression variables() {
        return variables;
    }

    /**
     * Defines a new variable
     *
     * @param name       the variable name
     * @param expression the value expression
     * @return this
     */
    public LetExpression variable(String name, Expression expression) {
        variables.field(name, expression);
        return this;
    }
}
