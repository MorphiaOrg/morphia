package dev.morphia.mapping.experimental;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class MorphiaReferenceCodec implements Codec<MorphiaReference> {

    private Mapper mapper;
    private BsonTypeClassMap bsonTypeClassMap;

    public MorphiaReferenceCodec(final Mapper mapper, final BsonTypeClassMap typeMap) {
        this.mapper = mapper;
        bsonTypeClassMap = typeMap;
    }

    Mapper getMapper() {
        return mapper;
    }

    @Override
    public Class<MorphiaReference> getEncoderClass() {
        return MorphiaReference.class;
    }

    @SuppressWarnings("unchecked")
    public MorphiaReference decode(final BsonReader reader, final DecoderContext decoderContext) {
        Object decode = mapper.getCodecRegistry()
                              .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                              .decode(reader, decoderContext);
        return MorphiaReference.wrapIds(mapper, decode);
    }

    @Override
    public void encode(final BsonWriter writer, final MorphiaReference value, final EncoderContext encoderContext) {
        Codec codec = mapper.getCodecRegistry().get(value.getClass());
        codec.encode(writer, value, encoderContext);
    }
}
