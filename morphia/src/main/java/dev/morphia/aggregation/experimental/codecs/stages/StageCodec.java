package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public abstract class StageCodec<T extends Stage> implements Codec<T> {
    private final Mapper mapper;

    protected StageCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    protected CodecRegistry getCodecRegistry() {
        return mapper.getCodecRegistry();
    }

    protected Mapper getMapper() {
        return mapper;
    }

    @Override
    public final T decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(value.getStageName());
        encodeStage(writer, value, encoderContext);
        writer.writeEndDocument();
    }

    protected abstract void encodeStage(final BsonWriter writer, final T value, final EncoderContext encoderContext);
}
