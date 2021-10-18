package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.IndexStats;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public class IndexStatsCodec extends StageCodec<IndexStats> {
    public IndexStatsCodec(Datastore datastore) {
        super(datastore);
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
