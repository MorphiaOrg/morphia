package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.CodecHelper;
import dev.morphia.query.filters.NearFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class NearFilterCodec extends BaseFilterCodec<NearFilter> {
    public NearFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, NearFilter near, EncoderContext encoderContext) {
        document(writer, near.path(datastore.getMapper()), () -> {
            if (near.isNot()) {
                document(writer, "$not", () -> {
                    encodeFilter(writer, near, encoderContext);
                });
            } else {
                encodeFilter(writer, near, encoderContext);
            }
        });

    }

    private void encodeFilter(BsonWriter writer, NearFilter near, EncoderContext context) {
        document(writer, near.getName(), () -> {
            writer.writeName("$geometry");
            CodecHelper.unnamedValue(writer, datastore, near.getValue(datastore), context);
            value(writer, "$minDistance", near.minDistance());
            value(writer, "$maxDistance", near.maxDistance());
            value(datastore.getCodecRegistry(), writer, "crs", near.crs(), context);
        });
    }

    @Override
    public Class<NearFilter> getEncoderClass() {
        return NearFilter.class;
    }
}
