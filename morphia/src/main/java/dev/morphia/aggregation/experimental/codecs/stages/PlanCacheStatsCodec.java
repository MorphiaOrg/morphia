package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.PlanCacheStats;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class PlanCacheStatsCodec extends StageCodec<PlanCacheStats> {
    public PlanCacheStatsCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<PlanCacheStats> getEncoderClass() {
        return PlanCacheStats.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final PlanCacheStats value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeEndDocument();
    }
}
