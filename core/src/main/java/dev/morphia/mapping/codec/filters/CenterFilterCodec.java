package dev.morphia.mapping.codec.filters;

import com.mongodb.client.model.geojson.Point;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.CenterFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;

public class CenterFilterCodec extends BaseFilterCodec<CenterFilter> {
    public CenterFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, CenterFilter filter, EncoderContext encoderContext) {
        document(writer, filter.path(datastore.getMapper()), () -> {
            document(writer, "$geoWithin", () -> {
                array(writer, filter.getName(), () -> {
                    array(writer, () -> {
                        Point center = filter.getValue();
                        for (Double value : center.getPosition().getValues()) {
                            writer.writeDouble(value);
                        }
                    });
                    writer.writeDouble(filter.radius());
                });
            });
        });
    }

    @Override
    public Class<CenterFilter> getEncoderClass() {
        return CenterFilter.class;
    }
}
