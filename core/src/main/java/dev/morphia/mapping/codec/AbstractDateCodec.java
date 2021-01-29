package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Date;

/**
 * An abstract class for {@link Date}-based codecs
 *
 * @param <DateT> the specific type of {@link Date}
 * @morphia.internal
 * @since 2.2
 */
public abstract class AbstractDateCodec<DateT extends Date> implements Codec<DateT> {

    public abstract Class<DateT> getEncoderClass();

    public abstract DateT decode(BsonReader reader, DecoderContext decoderContext);

    public void encode(BsonWriter writer, DateT value, EncoderContext encoderContext) {
        writer.writeDateTime(value.getTime());
    }

}
