package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.query.LegacyQuery;
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
public class LegacyQueryCodec implements Codec<LegacyQuery> {
    private final Datastore datastore;

    /**
     * Creates a codec
     *
     * @param datastore the Datastore to use
     */
    public LegacyQueryCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public LegacyQuery decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public void encode(BsonWriter writer, LegacyQuery value, EncoderContext encoderContext) {
        datastore.getCodecRegistry().get(Document.class).encode(writer, value.toDocument(), encoderContext);
    }

    @Override
    public Class<LegacyQuery> getEncoderClass() {
        return LegacyQuery.class;
    }
}
