package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

public class MatchCodec extends StageCodec<Match> {

    public MatchCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Match> getEncoderClass() {
        return Match.class;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void encodeStage(BsonWriter writer, Match value, EncoderContext encoderContext) {
        document(writer, () -> {
            for (Filter filter : value.getFilters()) {
                filter.encode(getDatastore(), writer, encoderContext);
            }
        });
    }
}
