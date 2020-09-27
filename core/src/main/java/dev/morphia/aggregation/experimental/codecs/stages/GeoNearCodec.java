package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.GeoNear;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class GeoNearCodec extends StageCodec<GeoNear> {
    public GeoNearCodec(Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<GeoNear> getEncoderClass() {
        return GeoNear.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, GeoNear value, EncoderContext encoderContext) {
        document(writer, () -> {
            value(getMapper(), writer, "near", value.getPoint(), encoderContext);
            value(getMapper(), writer, "near", value.getCoordinates(), encoderContext);
            value(getMapper(), writer, "key", value.getKey(), encoderContext);
            value(getMapper(), writer, "distanceField", value.getDistanceField(), encoderContext);
            value(getMapper(), writer, "spherical", value.getSpherical(), encoderContext);
            value(getMapper(), writer, "maxDistance", value.getMaxDistance(), encoderContext);
            value(getMapper(), writer, "minDistance", value.getMinDistance(), encoderContext);
            Filter[] filters = value.getFilters();
            if (filters != null) {
                document(writer, "query", () -> {
                    for (Filter filter : filters) {
                        filter.encode(getMapper(), writer, encoderContext);
                    }
                });
            }
            value(getMapper(), writer, "distanceMultiplier", value.getDistanceMultiplier(), encoderContext);
            value(getMapper(), writer, "includeLocs", value.getIncludeLocs(), encoderContext);
        });
    }
}
