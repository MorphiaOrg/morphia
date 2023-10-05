package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class FilterCodec extends BaseFilterCodec<Filter> {
    public FilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, Filter filter, EncoderContext encoderContext) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            if (filter.isNot()) {
                document(writer, "$not", () -> {
                    writeNamedValue(filter.getName(), filter.getValue(datastore), datastore, writer, encoderContext);
                });
            } else {
                writeNamedValue(filter.getName(), filter.getValue(datastore), datastore, writer, encoderContext);
            }
        });
    }

    @Override
    public Class<Filter> getEncoderClass() {
        return Filter.class;
    }
}
