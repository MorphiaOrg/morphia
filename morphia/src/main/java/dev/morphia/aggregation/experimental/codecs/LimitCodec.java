package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.Limit;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class LimitCodec implements Codec<Limit> {
    @Override
    public Limit decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(final BsonWriter writer, final Limit value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeInt32("$limit", value.getLimit());
        writer.writeEndDocument();
    }

    @Override
    public Class<Limit> getEncoderClass() {
        return Limit.class;
    }
}
