package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @since 2.1
 */
public class FunctionExpression extends Expression {
    private final String body;
    private final List<Expression> args;
    private final String lang = "js";

    /**
     * Creates the new expression
     *
     * @param body the function definition
     * @param args the funcation arguments
     * @morphia.internal
     */
    @MorphiaInternal
    public FunctionExpression(String body, List<Expression> args) {
        super("$function");
        this.body = body;
        this.args = args;
    }

    public String body() {
        return body;
    }

    public List<Expression> args() {
        return args;
    }

    public String lang() {
        return lang;
    }
}
