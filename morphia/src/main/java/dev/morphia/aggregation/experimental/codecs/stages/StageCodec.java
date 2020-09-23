package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

public abstract class StageCodec<T extends Stage> implements Codec<T> {
    private final Mapper mapper;

    protected StageCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    protected CodecRegistry getCodecRegistry() {
        return mapper.getCodecRegistry();
    }

    protected Mapper getMapper() {
        return mapper;
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public final void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        document(writer, () -> {
            writer.writeName(value.getStageName());
            encodeStage(writer, value, encoderContext);
        });
    }

    protected abstract void encodeStage(BsonWriter writer, T value, EncoderContext encoderContext);

}
