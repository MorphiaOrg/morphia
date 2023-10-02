package dev.morphia.aggregation.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.ExpressionHelper;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.query.Sort;
import dev.morphia.sofia.Sofia;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;

public class SortArrayExpression extends Expression {
    private final Expression input;

    private final Sort[] sort;

    public SortArrayExpression(Expression input, Sort... sort) {
        super("$sortArray");
        if (sort.length == 0) {
            throw new IllegalArgumentException(Sofia.atLeastOneSortRequired());
        }

        this.input = input;
        this.sort = sort;
    }

    public Expression input() {
        return input;
    }

    public Sort[] sort() {
        return sort;
    }

    @Override
    public void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        document(writer, operation(), () -> {
            expression(datastore, writer, "input", input, encoderContext);
            if (sort[0].getField().equals(Sort.NATURAL)) {
                ExpressionHelper.value(writer, "sortBy", sort[0].getOrder());
            } else {
                document(writer, "sortBy", () -> {
                    for (Sort s : sort) {
                        writer.writeInt64(s.getField(), s.getOrder());
                    }
                });
            }
        });
    }
}
