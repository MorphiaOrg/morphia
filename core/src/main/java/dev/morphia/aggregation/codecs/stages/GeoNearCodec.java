package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.GeoNear;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class GeoNearCodec extends StageCodec<GeoNear> {
    public GeoNearCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<GeoNear> getEncoderClass() {
        return GeoNear.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, GeoNear value, EncoderContext encoderContext) {
        document(writer, () -> {
            CodecRegistry registry = getDatastore().getCodecRegistry();
            value(registry, writer, "near", value.getPoint(), encoderContext);
            value(registry, writer, "near", value.getCoordinates(), encoderContext);
            value(writer, "key", value.getKey());
            value(writer, "distanceField", value.getDistanceField());
            value(writer, "spherical", value.getSpherical());
            value(registry, writer, "maxDistance", value.getMaxDistance(), encoderContext);
            value(registry, writer, "minDistance", value.getMinDistance(), encoderContext);
            Filter[] filters = value.getFilters();
            if (filters != null) {
                document(writer, "query", () -> {
                    for (Filter filter : filters) {
                        Codec codec = registry.get(filter.getClass());
                        codec.encode(writer, filter, encoderContext);
                    }
                });
            }
            value(registry, writer, "distanceMultiplier", value.getDistanceMultiplier(), encoderContext);
            value(writer, "includeLocs", value.getIncludeLocs());
        });
    }
}
