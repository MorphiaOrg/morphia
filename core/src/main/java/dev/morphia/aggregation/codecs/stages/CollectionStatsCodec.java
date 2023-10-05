package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.CollectionStats;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class CollectionStatsCodec extends StageCodec<CollectionStats> {
    public CollectionStatsCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<CollectionStats> getEncoderClass() {
        return CollectionStats.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, CollectionStats value, EncoderContext encoderContext) {
        document(writer, () -> {
            if (value.getHistogram()) {
                document(writer, "latencyStats", () -> writer.writeBoolean("histograms", true));
            }
            if (value.getScale() != null) {
                document(writer, "storageStats", () -> writer.writeInt32("scale", value.getScale()));
            }
            if (value.getCount()) {
                document(writer, "count", () -> {
                });
            }
        });
    }
}
