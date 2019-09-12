package dev.morphia.mapping.experimental;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class MapReferenceCodec implements Codec<MapReference> {
    private Mapper mapper;
    private BsonTypeClassMap typeMap;

    public MapReferenceCodec(final Mapper mapper, final BsonTypeClassMap typeMap) {
        this.mapper = mapper;
        this.typeMap = typeMap;
    }

    @Override
    public Class<MapReference> getEncoderClass() {
        return MapReference.class;
    }

    @SuppressWarnings("unchecked")
    public MapReference decode(final BsonReader reader, final DecoderContext decoderContext) {
        return (MapReference) MorphiaReference.wrap(mapper.getCodecRegistry()
                                                          .get(typeMap.get(reader.getCurrentBsonType()))
                                                          .decode(reader, decoderContext));
    }

    @Override
    public void encode(final BsonWriter writer, final MapReference value, final EncoderContext encoderContext) {
        if (value == null || value.getIds() == null) {
            writer.writeNull();
        } else {
            Object idValue = value.getIds();

            final Codec codec = mapper.getCodecRegistry().get(idValue.getClass());
            codec.encode(writer, idValue, encoderContext);
        }

    }
}
