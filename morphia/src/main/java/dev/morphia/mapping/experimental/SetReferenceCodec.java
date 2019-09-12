package dev.morphia.mapping.experimental;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class SetReferenceCodec implements Codec<SetReference> {
    private Mapper mapper;
    private BsonTypeClassMap typeMap;
    public SetReferenceCodec(final Mapper mapper, final BsonTypeClassMap typeMap) {
        this.mapper = mapper;
        this.typeMap = typeMap;
    }

    @Override
    public Class<SetReference> getEncoderClass() {
        return SetReference.class;
    }

    @SuppressWarnings("unchecked")
    public SetReference decode(final BsonReader reader, final DecoderContext decoderContext) {
        return (SetReference) MorphiaReference.wrap(mapper.getCodecRegistry()
                                                          .get(typeMap.get(reader.getCurrentBsonType()))
                                                          .decode(reader, decoderContext));
    }

    @Override
    public void encode(final BsonWriter writer, final SetReference value, final EncoderContext encoderContext) {
        if (value == null || value.getIds() == null) {
            writer.writeNull();
        } else {
            Object idValue = value.getIds();

            final Codec codec = mapper.getCodecRegistry().get(idValue.getClass());
            codec.encode(writer, idValue, encoderContext);
        }

    }
}
