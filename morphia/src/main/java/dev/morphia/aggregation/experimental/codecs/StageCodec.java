package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.stages.Stage;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public abstract class StageCodec<T extends Stage> implements Codec<T> {
    @Override
    public final T decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(value.getName());
        encodeStage(writer, value, encoderContext);
        writer.writeEndDocument();
    }

    protected abstract void encodeStage(final BsonWriter writer, final T value, final EncoderContext encoderContext);
}
