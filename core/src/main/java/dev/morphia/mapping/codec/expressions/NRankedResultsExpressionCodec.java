package dev.morphia.mapping.codec.expressions;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.NRankedResultsExpression;
import dev.morphia.query.Sort;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class NRankedResultsExpressionCodec extends BaseExpressionCodec<NRankedResultsExpression> {
    public NRankedResultsExpressionCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, NRankedResultsExpression ranked, EncoderContext encoderContext) {
        document(writer, ranked.operation(), () -> {
            CodecRegistry codecRegistry = datastore.getCodecRegistry();
            encodeIfNotNull(codecRegistry, writer, "output", ranked.output(), encoderContext);
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
            encodeIfNotNull(codecRegistry, writer, "n", ranked.n(), encoderContext);
        });
    }

    @Override
    public Class<NRankedResultsExpression> getEncoderClass() {
        return NRankedResultsExpression.class;
    }
}
