package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import dev.morphia.query.LegacyQuery;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Defines a codec for Query instances
 */
public class QueryCodec implements Codec<LegacyQuery> {
    private Mapper mapper;

    /**
     * Creates a codec
     *
     * @param mapper the mapper to use
     */
    public QueryCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LegacyQuery decode(final BsonReader reader, final DecoderContext decoderContext) {
        return null;
    }

    @Override
    public void encode(final BsonWriter writer, final LegacyQuery value, final EncoderContext encoderContext) {
        mapper.getCodecRegistry().get(Document.class).encode(writer, value.prepareQuery(), encoderContext);
    }

    @Override
    public Class<LegacyQuery> getEncoderClass() {
        return LegacyQuery.class;
    }
}
