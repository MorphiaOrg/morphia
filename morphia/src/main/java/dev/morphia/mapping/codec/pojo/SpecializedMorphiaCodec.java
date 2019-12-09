package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodec;

/**
 * A specialized form of a codec
 *
 * @param <T>
 */
public class SpecializedMorphiaCodec<T> extends PojoCodec<T> {

    private final MorphiaCodec morphiaCodec;
    private final EntityModel<T> classModel;
    private final Datastore datastore;
    private MappedClass mappedClass;
    private PojoCodec<T> specialized;

    public SpecializedMorphiaCodec(final MorphiaCodec morphiaCodec, final MappedClass mappedClass, final EntityModel<T> classModel,
                                   final Datastore datastore) {
        this.morphiaCodec = morphiaCodec;
        this.mappedClass = mappedClass;
        this.classModel = classModel;
        this.datastore = datastore;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return getSpecialized().decode(reader, decoderContext);
    }

    private PojoCodec<T> getSpecialized() {
        if (specialized == null) {
            specialized = new MorphiaCodec<>(datastore, mappedClass, morphiaCodec.getPropertyCodecRegistry(),
                morphiaCodec.getDiscriminatorLookup(), morphiaCodec.getCodecCache(), true, morphiaCodec.getRegistry());
        }
        return specialized;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        getSpecialized().encode(writer, value, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return classModel.getType();
    }

    @Override
    public ClassModel<T> getClassModel() {
        return classModel;
    }
}
