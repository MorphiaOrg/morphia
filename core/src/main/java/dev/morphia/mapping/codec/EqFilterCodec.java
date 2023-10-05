package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.EqFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class EqFilterCodec extends BaseFilterCodec<EqFilter> {
    public EqFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, EqFilter filter, EncoderContext encoderContext) {
        if (filter.isNot()) {
            document(writer, filter.path(datastore.getMapper()), () -> {
                document(writer, "$not", () -> {
                    writer.writeName(filter.getName());
                    writeUnnamedValue(filter.getValue(datastore), datastore, writer, encoderContext);
                });
            });
        } else {
            writeNamedValue(filter.path(datastore.getMapper()), filter.getValue(datastore), datastore, writer, encoderContext);
        }

    }

    @Override
    public Class<EqFilter> getEncoderClass() {
        return EqFilter.class;
    }
}
