package dev.morphia.mapping.codec.pojo;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecRegistry;

import java.util.concurrent.ConcurrentMap;

public class LazyMorphiaCodec<T> extends PojoCodec<T> {
    private final ClassModel<T> classModel;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache;
    private volatile PojoCodec<T> morphiaCodec;

    LazyMorphiaCodec(final ClassModel<T> classModel, final CodecRegistry registry, final PropertyCodecRegistry propertyCodecRegistry,
                     final DiscriminatorLookup discriminatorLookup, final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache) {
        this.classModel = classModel;
        this.registry = registry;
        this.propertyCodecRegistry = propertyCodecRegistry;
        this.discriminatorLookup = discriminatorLookup;
        this.codecCache = codecCache;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        getMorphiaCodec().encode(writer, value, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return classModel.getType();
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return getMorphiaCodec().decode(reader, decoderContext);
    }

    private Codec<T> getMorphiaCodec() {
        if (morphiaCodec == null) {
            morphiaCodec = new MorphiaCodec<T>(classModel, registry, propertyCodecRegistry, discriminatorLookup, true);
        }
        return morphiaCodec;
    }

    @Override
    public ClassModel<T> getClassModel() {
        return classModel;
    }
}
