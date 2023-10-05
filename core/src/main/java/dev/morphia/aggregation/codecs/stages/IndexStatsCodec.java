package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.IndexStats;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class IndexStatsCodec extends StageCodec<IndexStats> {
    public IndexStatsCodec(MorphiaDatastore datastore) {
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
