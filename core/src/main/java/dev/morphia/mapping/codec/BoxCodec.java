package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.Box;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.array;
import static dev.morphia.mapping.codec.CodecHelper.document;

public class BoxCodec extends BaseFilterCodec<Box> {
    public BoxCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, Box box, EncoderContext encoderContext) {
        document(writer, box.path(datastore.getMapper()), () -> {
            document(writer, "$geoWithin", () -> {
                array(writer, box.getName(), () -> {
                    array(writer, () -> {
                        for (Double value : box.bottomLeft().getPosition().getValues()) {
                            writer.writeDouble(value);
                        }
                    });
                    array(writer, () -> {
                        for (Double value : box.upperRight().getPosition().getValues()) {
                            writer.writeDouble(value);
                        }
                    });
                });
            });
        });

    }

    @Override
    public Class<Box> getEncoderClass() {
        return Box.class;
    }
}
