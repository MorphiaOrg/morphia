package dev.morphia.mapping.codec.pojo;

import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
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

public class LazyMorphiaCodec<T> extends PojoCodec<T> {
    private Mapper mapper;
    private MappedClass mappedClass;
    private final ClassModel<T> classModel;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private volatile PojoCodec<T> morphiaCodec;

    LazyMorphiaCodec(final Mapper mapper, final MappedClass mappedClass, final ClassModel<T> classModel, final CodecRegistry registry,
                     final PropertyCodecRegistry propertyCodecRegistry, final DiscriminatorLookup discriminatorLookup) {
        this.mapper = mapper;
        this.mappedClass = mappedClass;
        this.classModel = classModel;
        this.registry = registry;
        this.propertyCodecRegistry = propertyCodecRegistry;
        this.discriminatorLookup = discriminatorLookup;
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
            morphiaCodec = new MorphiaCodec<>(mapper, mappedClass, classModel, registry, propertyCodecRegistry, discriminatorLookup, true);
        }
        return morphiaCodec;
    }

    @Override
    public ClassModel<T> getClassModel() {
        return classModel;
    }
}
