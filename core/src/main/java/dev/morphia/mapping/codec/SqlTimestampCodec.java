package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.codecs.DecoderContext;

import java.sql.Timestamp;

/**
 * A codec for {@link Timestamp} instances.
 *
 * @morphia.internal
 * @since 2.2
 */
public class SqlTimestampCodec extends AbstractDateCodec<Timestamp> {

    @Override
    public Class<Timestamp> getEncoderClass() {
        return Timestamp.class;
    }

    @Override
    public Timestamp decode(BsonReader reader, DecoderContext decoderContext) {
        final long timestamp = reader.readDateTime();
        return new Timestamp(timestamp);
    }

}
