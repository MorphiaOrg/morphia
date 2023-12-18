package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.RegexFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class RegexFilterCodec extends BaseFilterCodec<RegexFilter> {
    public RegexFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, RegexFilter filter, EncoderContext encoderContext) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            if (filter.isNot()) {
                document(writer, "$not", () -> {
                    encodeFilter(writer, filter, encoderContext);
                });
            } else {
                encodeFilter(writer, filter, encoderContext);
            }
        });

    }

    private void encodeFilter(BsonWriter writer, RegexFilter filter, EncoderContext context) {
        value(writer, "$regex", filter.getValue().toString());
        value(writer, "$options", filter.options());
    }

    @Override
    public Class<RegexFilter> getEncoderClass() {
        return RegexFilter.class;
    }
}
