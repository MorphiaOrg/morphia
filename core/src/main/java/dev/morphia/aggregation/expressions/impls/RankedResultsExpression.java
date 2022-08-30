package dev.morphia.aggregation.expressions.impls;

import dev.morphia.Datastore;
import dev.morphia.query.Sort;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class RankedResultsExpression extends Expression {
    private final Expression output;
    private final Sort[] sortBy;

    public RankedResultsExpression(String operation, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.sortBy = sortBy;
    }

    static void encode(BsonWriter writer, Sort sort) {
        document(writer, () -> {
            writer.writeInt64(sort.getField(), sort.getOrder());
        });
    }

    @Override
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, getOperation(), () -> {
            expression(datastore, writer, "output", output, encoderContext);
            if (sortBy.length == 1) {
                writer.writeName("sortBy");

                encode(writer, sortBy[0]);
            } else {
                array(writer, "sortBy", () -> {
                    for (Sort sort : sortBy) {
                        encode(writer, sort);
                    }
                });
            }
        });
    }
}
