package dev.morphia.mapping.codec.filters;

import com.mongodb.client.model.geojson.Point;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.PolygonFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;

public class PolygonFilterCodec extends BaseFilterCodec<PolygonFilter> {
    public PolygonFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, PolygonFilter filter, EncoderContext encoderContext) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            document(writer, "$geoWithin", () -> {
                array(writer, "$polygon", () -> {
                    for (Point point : filter.points()) {
                        array(writer, () -> {
                            for (Double value : point.getPosition().getValues()) {
                                writer.writeDouble(value);
                            }
                        });
                    }
                });
            });
        });
    }

    @Override
    public Class<PolygonFilter> getEncoderClass() {
        return PolygonFilter.class;
    }
}
