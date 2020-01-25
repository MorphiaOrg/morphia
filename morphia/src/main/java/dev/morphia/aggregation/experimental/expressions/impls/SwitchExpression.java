package dev.morphia.aggregation.experimental.expressions.impls;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionCodec.writeNamedExpression;

/**
 * Evaluates a series of case expressions. When it finds an expression which evaluates to true, $switch executes a specified expression
 * and breaks out of the control flow.
 *
 * @since 2.0
 */
public class SwitchExpression extends Expression {
    List<Pair> branches = new ArrayList<>();
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
    public SwitchExpression branch(final Expression caseExpression, final Expression then) {
        branches.add(new Pair(caseExpression, then));
        return this;
    }

    /**
     * Adds a default case if nothing is matched.
     *
     * @param caseExpression the default case
     * @return this
     */
    public SwitchExpression defaultCase(final Expression caseExpression) {
        this.defaultCase = caseExpression;
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeStartDocument(getOperation());
        writer.writeStartArray("branches");
        for (final Pair branch : branches) {
            writer.writeStartDocument();
            writer.writeName("case");
            branch.caseExpression.encode(mapper, writer, encoderContext);
            writer.writeName("then");
            branch.then.encode(mapper, writer, encoderContext);
            writer.writeEndDocument();
        }
        writer.writeEndArray();
        writeNamedExpression(mapper, writer, "default", defaultCase, encoderContext);
        writer.writeEndDocument();
        writer.writeEndDocument();
    }

    private static class Pair {
        private final Expression caseExpression;
        private final Expression then;

        public Pair(final Expression caseExpression, final Expression then) {
            this.caseExpression = caseExpression;
            this.then = then;
        }
    }
}
