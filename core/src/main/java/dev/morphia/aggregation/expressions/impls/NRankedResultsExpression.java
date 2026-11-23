package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class NRankedResultsExpression extends Expression {
    private final Expression output;
    private final Expression n;
    private final Sort[] sortBy;

    public NRankedResultsExpression(String operation, Expression n, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.n = n;
        this.sortBy = sortBy;
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "output", output, encoderContext);
            // The server takes one sort document, its keys in priority order.
            writer.writeName("sortBy");
            document(writer, () -> {
                for (Sort sort : sortBy) {
                    writer.writeInt64(sort.getField(), sort.getOrder());
                }
            });
            expression(datastore, writer, "n", n, encoderContext);
        });
    }
}
