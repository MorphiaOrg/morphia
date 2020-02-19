package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Point;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Map;

public class NearFilter extends GeoFilter {
    private Double maxDistance;
    private Double minDistance;
    private CoordinateReferenceSystem crs;

    public NearFilter(final String filterName, final String field, final Point point) {
        super(filterName, field, point);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        if (isNot()) {
            writer.writeStartDocument("$not");
        }
        writer.writeStartDocument(getFilterName());
        writer.writeName("$geometry");
        writeUnnamedValue(getValue(mapper), mapper, writer, context);
        if (maxDistance != null) {
            writeNamedValue("$maxDistance", maxDistance, mapper, writer, context);
        }
        if (minDistance != null) {
            writeNamedValue("$minDistance", minDistance, mapper, writer, context);
        }
        if (crs != null) {
            writeNamedValue("crs", crs, mapper, writer, context);
        }
        writer.writeEndDocument();
        if (isNot()) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }

    public NearFilter maxDistance(final Double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    public NearFilter minDistance(final Double minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    public NearFilter crs(final CoordinateReferenceSystem crs) {
        this.crs = crs;
        return this;
    }

    /**
     * @param opts
     * @morphia.internal
     */
    public void applyOpts(final Map opts) {
        maxDistance = (Double) opts.get("$maxDistance");
        minDistance = (Double) opts.get("$minDistance");
    }

}
