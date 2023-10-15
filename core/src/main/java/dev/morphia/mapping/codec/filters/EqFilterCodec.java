package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.CodecHelper;
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
                    CodecHelper.unnamedValue(writer, datastore, filter.getValue(datastore), encoderContext);
                });
            });
        } else {
            CodecHelper.namedValue(writer, datastore, filter.path(datastore.getMapper()), filter.getValue(datastore), encoderContext);
        }

    }

    @Override
    public Class<EqFilter> getEncoderClass() {
        return EqFilter.class;
    }
}
