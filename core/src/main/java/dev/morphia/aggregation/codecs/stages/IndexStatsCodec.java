package dev.morphia.aggregation.codecs.stages;

import dev.morphia.aggregation.stages.IndexStats;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

public class IndexStatsCodec extends StageCodec<IndexStats> {
    @Override
    public Class<IndexStats> getEncoderClass() {
        return IndexStats.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, IndexStats value, EncoderContext encoderContext) {
        document(writer, () -> {
        });
    }
}
