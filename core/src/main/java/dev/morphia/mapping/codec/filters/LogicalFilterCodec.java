package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.LogicalFilter;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;

public class LogicalFilterCodec extends BaseFilterCodec<LogicalFilter> {
    public LogicalFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, LogicalFilter logicalFilter, EncoderContext encoderContext) {
        array(writer, logicalFilter.getName(), () -> {
            logicalFilter.filters().forEach(filter -> document(writer, () -> {
                Codec codec = datastore.getCodecRegistry().get(filter.getClass());
                codec.encode(writer, filter, encoderContext);
            }));
        });
    }

    @Override
    public Class<LogicalFilter> getEncoderClass() {
        return LogicalFilter.class;
    }
}
