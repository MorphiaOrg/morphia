package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.IndexStats;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class IndexStatsCodec extends StageCodec<IndexStats> {
    public IndexStatsCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<IndexStats> getEncoderClass() {
        return IndexStats.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final IndexStats value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeEndDocument();
    }
}
