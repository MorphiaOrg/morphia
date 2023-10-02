package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.RankedResultsExpression;
import dev.morphia.query.Sort;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.expression;
import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class RankedResultsExpressionCodec extends BaseExpressionCodec<RankedResultsExpression> {
    public RankedResultsExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, RankedResultsExpression ranked, EncoderContext encoderContext) {
        document(writer, ranked.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "output", ranked.output(), encoderContext);
            Sort[] sortBy = ranked.sortBy();
            if (sortBy.length == 1) {
                writer.writeName("sortBy");

                document(writer, () -> {
                    writer.writeInt64(sortBy[0].getField(), sortBy[0].getOrder());
                });
            } else {
                array(writer, "sortBy", () -> {
                    for (Sort sort : sortBy) {
                        document(writer, () -> {
                            writer.writeInt64(sort.getField(), sort.getOrder());
                        });
                    }
                });
            }
        });
    }

    @Override
    public Class<RankedResultsExpression> getEncoderClass() {
        return RankedResultsExpression.class;
    }
}
