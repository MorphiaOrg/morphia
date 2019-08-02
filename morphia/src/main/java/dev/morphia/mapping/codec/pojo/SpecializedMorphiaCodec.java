package dev.morphia.mapping.codec.pojo;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodec;

public class SpecializedMorphiaCodec<T> extends PojoCodec<T> {

    private final MorphiaCodec morphiaCodec;
    private final ClassModel<T> classModel;
    private PojoCodec<T> specialized;

    SpecializedMorphiaCodec(final MorphiaCodec morphiaCodec, final ClassModel<T> classModel) {
        this.morphiaCodec = morphiaCodec;
        this.classModel = classModel;
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
            specialized = new MorphiaCodec<>(morphiaCodec.getMapper(), morphiaCodec.getMappedClass(), classModel,
                morphiaCodec.getRegistry(), morphiaCodec.getPropertyCodecRegistry(), morphiaCodec.getDiscriminatorLookup(), true);
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
