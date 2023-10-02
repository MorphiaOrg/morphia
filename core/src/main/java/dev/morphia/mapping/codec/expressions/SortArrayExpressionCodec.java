package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.SortArrayExpression;
import dev.morphia.query.Sort;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class SortArrayExpressionCodec extends BaseExpressionCodec<SortArrayExpression> {
    public SortArrayExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SortArrayExpression expression, EncoderContext encoderContext) {
        document(writer, expression.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "input", expression.input(), encoderContext);
            Sort[] sort = expression.sort();
            if (sort[0].getField().equals(Sort.NATURAL)) {
                writer.writeInt32("sortBy", sort[0].getOrder());
            } else {
                document(writer, "sortBy", () -> {
                    for (Sort s : sort) {
                        writer.writeInt64(s.getField(), s.getOrder());
                    }
                });
            }
        });

    }

    @Override
    public Class<SortArrayExpression> getEncoderClass() {
        return SortArrayExpression.class;
    }
}
