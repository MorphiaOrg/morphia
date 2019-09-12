package dev.morphia.mapping.experimental;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class SingleReferenceCodec implements Codec<SingleReference> {
    private final Mapper mapper;
    private final BsonTypeClassMap typeMap;

    public SingleReferenceCodec(final Mapper mapper, final BsonTypeClassMap typeMap) {
        this.mapper = mapper;
        this.typeMap = typeMap;
    }

    @Override
    public Class<SingleReference> getEncoderClass() {
        return SingleReference.class;
    }

    @SuppressWarnings("unchecked")
    public SingleReference decode(final BsonReader reader, final DecoderContext decoderContext) {
        return (SingleReference) MorphiaReference.wrap(mapper.getCodecRegistry()
                                                             .get(typeMap.get(reader.getCurrentBsonType()))
                                                             .decode(reader, decoderContext));
    }

    @Override
    public void encode(final BsonWriter writer, final SingleReference value, final EncoderContext encoderContext) {
        if (value == null || value.getId() == null) {
            writer.writeNull();
        } else {
            Object idValue = value.getId();

            final Codec codec = mapper.getCodecRegistry().get(idValue.getClass());
            codec.encode(writer, idValue, encoderContext);
        }

    }
}
