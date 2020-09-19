package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class CollectionStatsCodec extends StageCodec<CollectionStats> {
    public CollectionStatsCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<CollectionStats> getEncoderClass() {
        return CollectionStats.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, CollectionStats value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        if (value.getHistogram()) {
            writer.writeStartDocument("latencyStats");
            writer.writeBoolean("histograms", true);
            writer.writeEndDocument();
        }
        if (value.getScale() != null) {
            writer.writeStartDocument("storageStats");
            writer.writeInt32("scale", value.getScale());
            writer.writeEndDocument();
        }
        if (value.getCount()) {
            writer.writeStartDocument("count");
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }
}
