package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.CodecHelper;
import dev.morphia.query.filters.FieldLessFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

public class FieldLessFilterCodec extends BaseFilterCodec<FieldLessFilter> {
    public FieldLessFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, FieldLessFilter filter, EncoderContext encoderContext) {
        if (filter.isNot()) {
            document(writer, "$not", () -> {
                CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(datastore), encoderContext);
            });
        } else {
            CodecHelper.namedValue(writer, datastore, filter.getName(), filter.getValue(datastore), encoderContext);
        }
    }

    @Override
    public Class<FieldLessFilter> getEncoderClass() {
        return FieldLessFilter.class;
    }
}
