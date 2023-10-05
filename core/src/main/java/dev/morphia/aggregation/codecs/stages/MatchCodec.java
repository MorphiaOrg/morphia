package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class MatchCodec extends StageCodec<Match> {

    public MatchCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Match> getEncoderClass() {
        return Match.class;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void encodeStage(BsonWriter writer, Match value, EncoderContext encoderContext) {
        document(writer, () -> {
            for (Filter filter : value.getFilters()) {
                Codec codec = getCodecRegistry().get(filter.getClass());
                codec.encode(writer, filter, encoderContext);
            }
        });
    }
}
