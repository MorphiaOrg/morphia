package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class MatchCodec extends StageCodec<Match> {

    public MatchCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void encodeStage(BsonWriter writer, Match value, EncoderContext encoderContext) {
        Filter[] filters = value.getFilters();
        writer.writeStartDocument();
        for (Filter filter : filters) {
            filter.encode(getMapper(), writer, encoderContext);
        }
        writer.writeEndDocument();
    }

    @Override
    public Class<Match> getEncoderClass() {
        return Match.class;
    }
}
