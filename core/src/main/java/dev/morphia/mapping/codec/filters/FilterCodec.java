package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.CodecHelper;
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
        String path = filter.path(datastore.getMapper());
        if (!path.equals("")) {
            document(writer, path, () -> {
                if (filter.isNot()) {
                    document(writer, "$not", () -> {
                        CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(datastore), encoderContext);
                    });
                } else {
                    CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(datastore), encoderContext);
                }
            });
        } else {
            document(writer, () -> {
                if (filter.isNot()) {
                    document(writer, "$not", () -> {
                        CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(datastore), encoderContext);
                    });
                } else {
                    CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(datastore), encoderContext);
                }
            });
        }
    }

    @Override
    public Class<Filter> getEncoderClass() {
        return Filter.class;
    }
}
