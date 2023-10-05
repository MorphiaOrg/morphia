package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.WhereFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static java.lang.String.format;

public class WhereFilterCodec extends BaseFilterCodec<WhereFilter> {
    public WhereFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, WhereFilter filter, EncoderContext encoderContext) {
        writer.writeName(filter.getName());
        Object where = filter.getValue();
        if (where != null) {
            String value = where.toString().trim();
            if (!value.startsWith("function()")) {
                value = format("function() { %s }", value);
            }
            writer.writeString(value);
        } else {
            writer.writeNull();
        }
    }

    @Override
    public Class<WhereFilter> getEncoderClass() {
        return WhereFilter.class;
    }
}
