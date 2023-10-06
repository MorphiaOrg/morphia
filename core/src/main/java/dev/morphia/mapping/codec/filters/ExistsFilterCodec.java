package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.ExistsFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class ExistsFilterCodec extends BaseFilterCodec<ExistsFilter> {
    public ExistsFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ExistsFilter filter, EncoderContext encoderContext) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            writer.writeBoolean(filter.getName(), !filter.isNot());
        });

    }

    @Override
    public Class<ExistsFilter> getEncoderClass() {
        return ExistsFilter.class;
    }
}
