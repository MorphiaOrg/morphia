package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

public class NoOpMorphiaPropertyCodecProvider extends MorphiaPropertyCodecProvider {

    @Override
    public <T> Codec<T> get(TypeWithTypeParameters<T> typeWithTypeParameters,
            PropertyCodecRegistry propertyCodecRegistry) {
        if (NoOpClass.class.isAssignableFrom(typeWithTypeParameters.getType())) {
            return (Codec<T>) new NoOpCodec();
        }

        return null;
    }

    static class NoOpCodec implements Codec<NoOpClass> {

        @Override
        public NoOpClass decode(BsonReader bsonReader, DecoderContext decoderContext) {
            return null;
        }

        @Override
        public void encode(BsonWriter bsonWriter, NoOpClass unused, EncoderContext encoderContext) {

        }

        @Override
        public Class<NoOpClass> getEncoderClass() {
            return NoOpClass.class;
        }
    }

    static class NoOpClass {
    }
}
