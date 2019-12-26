package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Query;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

public class MatchCodec extends StageCodec<Match> {

    public MatchCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void encodeStage(final BsonWriter writer, final Match value, final EncoderContext encoderContext) {
        Query query = value.getQuery();
        Codec codec = getCodecRegistry().get(query.getClass());
        encoderContext.encodeWithChildContext(codec, writer, value.getQuery());
    }

    @Override
    public Class<Match> getEncoderClass() {
        return Match.class;
    }
}
