package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.GeoNear;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class GeoNearCodec extends StageCodec<GeoNear> {
    public GeoNearCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class<GeoNear> getEncoderClass() {
        return GeoNear.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, GeoNear value, EncoderContext encoderContext) {
        document(writer, () -> {
            value(getDatastore(), writer, "near", value.getPoint(), encoderContext);
            value(getDatastore(), writer, "near", value.getCoordinates(), encoderContext);
            value(writer, "key", value.getKey());
            value(writer, "distanceField", value.getDistanceField());
            value(writer, "spherical", value.getSpherical());
            value(getDatastore(), writer, "maxDistance", value.getMaxDistance(), encoderContext);
            value(getDatastore(), writer, "minDistance", value.getMinDistance(), encoderContext);
            Filter[] filters = value.getFilters();
            if (filters != null) {
                document(writer, "query", () -> {
                    for (Filter filter : filters) {
                        filter.encode(getDatastore(), writer, encoderContext);
                    }
                });
            }
            value(getDatastore(), writer, "distanceMultiplier", value.getDistanceMultiplier(), encoderContext);
            value(writer, "includeLocs", value.getIncludeLocs());
        });
    }
}
