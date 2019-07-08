package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ObjectCodec implements Codec<Object> {

    private Mapper mapper;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();

    public ObjectCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext decoderContext) {
        return mapper.getCodecRegistry()
                     .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                     .decode(reader, decoderContext);
    }

    @Override
    public void encode(final BsonWriter writer, final Object value, final EncoderContext encoderContext) {
        final Codec codec = mapper.getCodecRegistry().get(value.getClass());
        codec.encode(writer, value, encoderContext);
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
    }
}
