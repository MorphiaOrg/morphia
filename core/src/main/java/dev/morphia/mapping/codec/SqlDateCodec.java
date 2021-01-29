package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.codecs.DecoderContext;

import java.sql.Date;

/**
 * A codec for {@link Date} instances.
 *
 * @morphia.internal
 * @since 2.2
 */
public class SqlDateCodec extends AbstractDateCodec<Date> {

    @Override
    public Date decode(BsonReader reader, DecoderContext decoderContext) {
        final long dateTime = reader.readDateTime();
        return new Date(dateTime);
    }

    @Override
    public Class<Date> getEncoderClass() {
        return Date.class;
    }

}
