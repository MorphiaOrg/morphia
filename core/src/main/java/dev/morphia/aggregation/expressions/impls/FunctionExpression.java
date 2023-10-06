package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @since 2.1
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class FunctionExpression extends Expression {
    private final String body;
    private final List<Expression> args;
    private final String lang = "js";

    /**
     * Creates the new expression
     *
     * @param body the function definition
     * @param args the function arguments
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public FunctionExpression(String body, List<Expression> args) {
        super("$function");
        this.body = body;
        this.args = args;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the function body
     */
    @MorphiaInternal
    public String body() {
        return body;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the arguments
     */
    @MorphiaInternal
    public List<Expression> args() {
        return args;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the lang
     */
    @MorphiaInternal
    public String lang() {
        return lang;
    }
}
