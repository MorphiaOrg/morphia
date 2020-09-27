package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Defines a codec for Query instances
 */
public class MorphiaQueryCodec implements Codec<MorphiaQuery> {
    private final Mapper mapper;

    /**
     * Creates a codec
     *
     * @param mapper the mapper to use
     */
    public MorphiaQueryCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MorphiaQuery<?> decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(BsonWriter writer, MorphiaQuery value, EncoderContext encoderContext) {
        mapper.getCodecRegistry().get(Document.class).encode(writer, value.toDocument(), encoderContext);
    }

    @Override
    public Class<MorphiaQuery> getEncoderClass() {
        return MorphiaQuery.class;
    }
}
