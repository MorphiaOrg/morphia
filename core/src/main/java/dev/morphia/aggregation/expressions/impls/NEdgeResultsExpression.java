package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.query.Sort;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class NEdgeResultsExpression extends Expression {
    private final Expression output;
    private final Expression n;
    private final Sort[] sortBy;

    public NEdgeResultsExpression(String operation, Expression n, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.n = n;
        this.sortBy = sortBy;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "output", output, encoderContext);
            if (sortBy.length == 1) {
                writer.writeName("sortBy");

                EdgeResultsExpression.encode(writer, sortBy[0]);
            } else {
                array(writer, "sortBy", () -> {
                    for (Sort sort : sortBy) {
                        EdgeResultsExpression.encode(writer, sort);
                    }
                });
            }
            expression(datastore, writer, "n", n, encoderContext);
        });
    }
}
