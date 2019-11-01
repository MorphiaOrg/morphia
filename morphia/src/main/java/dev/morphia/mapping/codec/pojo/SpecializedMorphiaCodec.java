package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodec;

/**
 * A specialized form of a codec
 * @param <T>
 */
public class SpecializedMorphiaCodec<T> extends PojoCodec<T> {

    private final MorphiaCodec morphiaCodec;
    private final ClassModel<T> classModel;
    private final Datastore datastore;
    private PojoCodec<T> specialized;

    SpecializedMorphiaCodec(final MorphiaCodec morphiaCodec, final ClassModel<T> classModel, final Datastore datastore) {
        this.morphiaCodec = morphiaCodec;
        this.classModel = classModel;
        this.datastore = datastore;
    }

    @Override
    public ClassModel<T> getClassModel() {
        return classModel;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return getSpecialized().decode(reader, decoderContext);
    }

    private PojoCodec<T> getSpecialized() {
        if (specialized == null) {
            specialized = new MorphiaCodec<>(datastore, classModel,
                morphiaCodec.getRegistry(), morphiaCodec.getPropertyCodecRegistry(), morphiaCodec.getDiscriminatorLookup(), true,
                morphiaCodec.getMappedClass()
            );
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
}
