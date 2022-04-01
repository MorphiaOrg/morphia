package dev.morphia.aggregation.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

public abstract class StageCodec<T extends Stage> implements Codec<T> {
    private final Datastore datastore;

    protected StageCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public final T decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.encodingOnly());
    }

    @Override
    public final void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        document(writer, () -> {
            writer.writeName(value.stageName());
            encodeStage(writer, value, encoderContext);
        });
    }

    protected abstract void encodeStage(BsonWriter writer, T value, EncoderContext encoderContext);

    public Datastore getDatastore() {
        return datastore;
    }

    protected CodecRegistry getCodecRegistry() {
        return datastore.getCodecRegistry();
    }
}
