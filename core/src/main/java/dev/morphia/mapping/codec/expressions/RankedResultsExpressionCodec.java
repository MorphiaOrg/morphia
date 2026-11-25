package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.RankedResultsExpression;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class RankedResultsExpressionCodec extends BaseExpressionCodec<RankedResultsExpression> {
    public RankedResultsExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, RankedResultsExpression ranked, EncoderContext encoderContext) {
        document(writer, ranked.operation(), () -> {
            encodeIfNotNull(datastore.getCodecRegistry(), writer, "output", ranked.output(), encoderContext);
            // The server takes one sort document, its keys in priority order.
            writer.writeName("sortBy");
            document(writer, () -> {
                for (Sort sort : ranked.sortBy()) {
                    writer.writeInt32(sort.getField(), sort.getOrder());
                }
            });
        });
    }

    @Override
    public Class<RankedResultsExpression> getEncoderClass() {
        return RankedResultsExpression.class;
    }
}
