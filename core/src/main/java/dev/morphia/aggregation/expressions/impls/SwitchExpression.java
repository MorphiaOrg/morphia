package dev.morphia.aggregation.expressions.impls;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.codecs.ExpressionHelper.wrapExpression;

/**
 * Evaluates a series of case expressions. When it finds an expression which evaluates to true, $switch executes a specified expression
 * and breaks out of the control flow.
 *
 * @since 2.0
 */
public class SwitchExpression extends Expression {
    private final List<Pair> branches = new ArrayList<>();
    private Expression defaultCase;

    /**
     * @morphia.internal
     */
    @MorphiaInternal
    public SwitchExpression() {
        super("$switch");
    }

    /**
     * Adds a new branch to the switch
     *
     * @param caseExpression Can be any valid expression that resolves to a boolean. If the result is not a boolean, it is coerced to a
     *                       boolean value.
     * @param then           the expression to evaluate if the case is true
     * @return this
     */
    public SwitchExpression branch(Expression caseExpression, Expression then) {
        branches.add(new Pair(caseExpression, then));
        return this;
    }

    /**
     * Adds a default case if nothing is matched.
     *
     * @param caseExpression the default case
     * @return this
     */
    public SwitchExpression defaultCase(Expression caseExpression) {
        this.defaultCase = caseExpression;
        return this;
    }

    public List<Pair> branches() {
        return branches;
    }

    public Expression defaultCase() {
        return defaultCase;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }

    public static class Pair {
        private final Expression caseExpression;
        private final Expression then;

        Pair(Expression caseExpression, Expression then) {
            this.caseExpression = caseExpression;
            this.then = then;
        }

        public Expression caseExpression() {
            return caseExpression;
        }

        public Expression then() {
            return then;
        }
    }
}
