package dev.morphia.aggregation.expressions.impls;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class RankedResultsExpression extends Expression {
    private final Expression output;
    private final Sort[] sortBy;

    public RankedResultsExpression(String operation, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.sortBy = sortBy;
    }

    public Expression output() {
        return output;
    }

    public Sort[] sortBy() {
        return sortBy;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
        /*
         * document(writer, operation(), () -> {
         * expression(datastore, writer, "output", output, encoderContext);
         * if (sortBy.length == 1) {
         * writer.writeName("sortBy");
         * 
         * ExpressionHelper.encode(writer, sortBy[0]);
         * } else {
         * array(writer, "sortBy", () -> {
         * for (Sort sort : sortBy) {
         * ExpressionHelper.encode(writer, sort);
         * }
         * });
         * }
         * });
         */
    }
}
