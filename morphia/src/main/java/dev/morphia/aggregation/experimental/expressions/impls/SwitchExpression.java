package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;

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

    @Override
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, getOperation(), () -> {
                array(writer, "branches", () -> {
                    for (Pair branch : branches) {
                        document(writer, () -> {
                            expression(mapper, writer, "case", branch.caseExpression, encoderContext);
                            expression(mapper, writer, "then", branch.then, encoderContext);
                        });
                    }
                });
                expression(mapper, writer, "default", defaultCase, encoderContext);
            });
        });
    }

    private static class Pair {
        private final Expression caseExpression;
        private final Expression then;

        public Pair(Expression caseExpression, Expression then) {
            this.caseExpression = caseExpression;
            this.then = then;
        }
    }
}
