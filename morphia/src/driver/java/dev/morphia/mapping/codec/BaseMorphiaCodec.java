package dev.morphia.mapping.codec;

import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.LazyPojoCodec;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PojoCodecImpl;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.PropertyModel;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

public class BaseMorphiaCodec<T> extends PojoCodecImpl<T> {
    public BaseMorphiaCodec(final ClassModel<T> classModel,
                            final CodecRegistry registry,
                            final List<PropertyCodecProvider> propertyCodecProviders,
                            final DiscriminatorLookup discriminatorLookup) {
        super(classModel, registry, propertyCodecProviders, discriminatorLookup);
    }

    public BaseMorphiaCodec(final ClassModel<T> classModel,
                            final CodecRegistry registry,
                            final PropertyCodecRegistry propertyCodecRegistry,
                            final DiscriminatorLookup discriminatorLookup,
                            final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache,
                            final boolean specialized) {
        super(classModel, registry, propertyCodecRegistry, discriminatorLookup, codecCache, specialized);
    }

    @Override
    protected <T1> boolean shouldSpecialize(final ClassModel<T1> classModel) {
        return true;
    }


/* @Override
    protected void specialize() {
        ClassModel<T> classModel = getClassModel();
        getCodecCache().put(classModel, this);
        for (PropertyModel<?> propertyModel : classModel.getPropertyModels()) {
            addToCache(propertyModel);
        }
    }*/

    @SuppressWarnings("unchecked")
    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
/*
        if (!isSpecialized()) {
            throw new CodecConfigurationException(format("%s contains generic types that have not been specialised.%n"
                                                         + "Top level classes with generic types are not supported by the PojoCodec.", getClassModel().getName()));
        }
*/

        if (areEquivalentTypes(value.getClass(), getClassModel().getType())) {
            writer.writeStartDocument();

            encodeIdProperty(writer, value, encoderContext, getClassModel().getIdPropertyModelHolder());

            if (/*encoderContext.isEncodingCollectibleDocument() && */getClassModel().useDiscriminator()) {
                writer.writeString(getClassModel().getDiscriminatorKey(), getClassModel().getDiscriminator());
            }

            for (PropertyModel<?> propertyModel : getClassModel().getPropertyModels()) {
                if (propertyModel.equals(getClassModel().getIdPropertyModel())) {
                    continue;
                }
                encodeProperty(writer, value, encoderContext, propertyModel);
            }
            writer.writeEndDocument();
        } else {
            specializeCodec((Class<T>) value.getClass()).encode(writer, value, encoderContext);
        }
    }
    /**
     * Creates a specialized codec that honors the current useDiscriminator setting.  For use in case where the field's declared type is
     * a parent type but the actual value is a subtype.
     *
     * @param type the actual type
     * @return potentially specialized Codec
     */
    private Codec<T> specializeCodec(final Class<T> type) {
        Codec<T> codec = getRegistry().get(type);
        if(codec instanceof BaseMorphiaCodec) {
            final BaseMorphiaCodec<T> tCodec = (BaseMorphiaCodec<T>) codec;
            final ClassModel<T> clazzModel = tCodec.getClassModel();
            final ClassModel<T> newModel = new ClassModel<>(clazzModel.getType(), clazzModel.getPropertyNameToTypeParameterMap(),
                clazzModel.getInstanceCreatorFactory(), getClassModel().useDiscriminator(), clazzModel.getDiscriminatorKey(),
                clazzModel.getDiscriminator(), clazzModel.getIdPropertyModelHolder(), clazzModel.getAnnotations(),
                clazzModel.getFieldModels(),
                clazzModel.getPropertyModels());
            codec = tCodec.getSpecializedCodec(newModel);
        }
        return codec;
    }

    protected <S> void decodePropertyModel(final BsonReader reader, final DecoderContext decoderContext,
                                           final InstanceCreator<T> instanceCreator, final String name,
                                           final PropertyModel<S> propertyModel) {
        if (propertyModel != null) {
            try {
                S value = null;
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
/*
                    final DecoderContext propertyContext
                        = DecoderContext.builder(decoderContext)
                                        .checkedDiscriminator(decoderContext.hasCheckedDiscriminator() ||
                                                              Boolean.TRUE.equals(propertyModel.useDiscriminator()))
                                        .metaData("discriminatorKey", getClassModel().getDiscriminatorKey())
                                        .build();
                    value = propertyModel.getCachedCodec().decode(reader, propertyContext);
*/
                    value = decoderContext.decodeWithChildContext(propertyModel.getCachedCodec(), reader);
                }
                if (propertyModel.isWritable()) {
                    instanceCreator.set(value, propertyModel);
                }
            } catch (IllegalArgumentException | BsonInvalidOperationException | CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. Decoding '%s' errored with: %s",
                    getClassModel().getName(), name, e.getMessage()), e);
            }
        } else {
            reader.skipValue();
        }
    }

    protected <S> PojoCodec<S> getSpecializedCodec(final ClassModel<S> specialized) {
        return new LazyPojoCodec<>(specialized, getRegistry(), getPropertyCodecRegistry(), getDiscriminatorLookup(), getCodecCache());
    }

}
