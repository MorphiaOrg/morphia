package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.CommentFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.namedValue;

public class CommentFilterCodec extends BaseFilterCodec<CommentFilter> {
    public CommentFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, CommentFilter value, EncoderContext encoderContext) {
        namedValue(writer, datastore, value.getName(), value.comment(), encoderContext);
    }

    @Override
    public Class<CommentFilter> getEncoderClass() {
        return CommentFilter.class;
    }
}
