package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.ModFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.unnamedValue;
import static dev.morphia.mapping.codec.CodecHelper.whole;

public class ModFilterCodec extends BaseFilterCodec<ModFilter> {
    public ModFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ModFilter filter, EncoderContext context) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            array(writer, filter.getName(), () -> {
                number(writer, filter.divisor(), context);
                number(writer, filter.remainder(), context);
            });
        });
    }

    /** A long operand (the integral overload of {@code mod}) is written as {@code whole} writes it. */
    private void number(BsonWriter writer, Number value, EncoderContext context) {
        if (value instanceof Long integral) {
            whole(writer, integral);
        } else {
            unnamedValue(writer, datastore, value, context);
        }
    }

    @Override
    public Class<ModFilter> getEncoderClass() {
        return ModFilter.class;
    }
}
