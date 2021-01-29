package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.codecs.DecoderContext;

import java.sql.Date;
import java.sql.Time;

/**
 * A codec for {@link Time} instances.
 *
 * @morphia.internal
 * @since 2.2
 */
public class SqlTimeCodec extends AbstractDateCodec<Time> {

    @Override
    public Class<Time> getEncoderClass() {
        return Time.class;
    }

    @Override
    public Time decode(BsonReader reader, DecoderContext decoderContext) {
        final long time = reader.readDateTime();
        return new Time(time);
    }

}
