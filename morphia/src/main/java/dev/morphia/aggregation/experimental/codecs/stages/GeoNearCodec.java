package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.GeoNear;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

public class GeoNearCodec extends StageCodec<GeoNear> {
    public GeoNearCodec(final Mapper mapper) {
        super(mapper);
    }

    @Override
    public Class<GeoNear> getEncoderClass() {
        return GeoNear.class;
    }

    @Override
    protected void encodeStage(final BsonWriter writer, final GeoNear value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writeNamedValue(writer, "near", value.getPoint(), encoderContext);
        writeNamedValue(writer, "near", value.getCoordinates(), encoderContext);
        writeNamedValue(writer, "key", value.getKey(), encoderContext);
        writeNamedValue(writer, "distanceField", value.getDistanceField(), encoderContext);
        writeNamedValue(writer, "spherical", value.getSpherical(), encoderContext);
        writeNamedValue(writer, "maxDistance", value.getMaxDistance(), encoderContext);
        writeNamedValue(writer, "minDistance", value.getMinDistance(), encoderContext);
        Filter[] filters = value.getFilters();
        if (filters != null) {
            writer.writeStartDocument("query");
            for (final Filter filter : filters) {
                filter.encode(getMapper(), writer, encoderContext);
            }
            writer.writeEndDocument();
        }
        writeNamedValue(writer, "distanceMultiplier", value.getDistanceMultiplier(), encoderContext);
        writeNamedValue(writer, "includeLocs", value.getIncludeLocs(), encoderContext);
        writer.writeEndDocument();
    }
}
