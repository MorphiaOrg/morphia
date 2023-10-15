package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.ModFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.unnamedValue;

public class ModFilterCodec extends BaseFilterCodec<ModFilter> {
    public ModFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, ModFilter filter, EncoderContext context) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            array(writer, filter.getName(), () -> {
                unnamedValue(writer, datastore, filter.divisor(), context);
                unnamedValue(writer, datastore, filter.remainder(), context);
            });
        });
    }

    @Override
    public Class<ModFilter> getEncoderClass() {
        return ModFilter.class;
    }
}
