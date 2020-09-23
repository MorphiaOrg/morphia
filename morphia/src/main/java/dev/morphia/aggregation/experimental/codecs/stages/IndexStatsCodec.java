package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.IndexStats;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class IndexStatsCodec extends StageCodec<IndexStats> {
    public IndexStatsCodec(Mapper mapper) {
        super(mapper);
    }

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
