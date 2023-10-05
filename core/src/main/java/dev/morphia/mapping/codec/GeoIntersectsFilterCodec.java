package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.GeoIntersectsFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class GeoIntersectsFilterCodec extends BaseFilterCodec<GeoIntersectsFilter> {
    public GeoIntersectsFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, GeoIntersectsFilter value, EncoderContext encoderContext) {
        document(writer, value.path(datastore.getMapper()), () -> {
            if (value.isNot()) {
                document(writer, "$not", () -> {
                    encodeFilter(writer, value, encoderContext);
                });
            } else {
                encodeFilter(writer, value, encoderContext);
            }
        });
    }

    private void encodeFilter(BsonWriter writer, GeoIntersectsFilter value, EncoderContext encoderContext) {
        document(writer, value.getName(), () -> {
            value(datastore.getCodecRegistry(), writer, "$geometry", value.getValue(datastore), encoderContext);
        });
    }

    @Override
    public Class<GeoIntersectsFilter> getEncoderClass() {
        return GeoIntersectsFilter.class;
    }
}
