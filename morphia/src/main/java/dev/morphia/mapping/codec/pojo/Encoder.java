package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.IdPropertyModelHolder;
import org.bson.codecs.pojo.PropertyModel;

import java.util.Collection;
import java.util.Map;

class Encoder<T> implements org.bson.codecs.Encoder<T> {
    private final MorphiaCodec<T> morphiaCodec;

    public Encoder(final MorphiaCodec<T> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        if (morphiaCodec.getMappedClass().hasLifecycle(PostPersist.class)
            || morphiaCodec.getMappedClass().hasLifecycle(PrePersist.class)
            || morphiaCodec.getMapper().hasInterceptors()) {

            encodeWithLifecycle(writer, value, encoderContext);
        } else {
            encodeEntity(writer, value, encoderContext);
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return morphiaCodec.getEncoderClass();
    }

    protected <S, V> boolean areEquivalentTypes(final Class<S> t1, final Class<V> t2) {
        return t1.equals(t2)
               || Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)
               || Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2);
    }

    protected <S> void encodeIdProperty(final BsonWriter writer, final T instance, final EncoderContext encoderContext,
                                        final IdPropertyModelHolder<S> propertyModelHolder) {
        if (propertyModelHolder.getPropertyModel() != null) {
            if (propertyModelHolder.getIdGenerator() == null) {
                encodeProperty(writer, instance, encoderContext, propertyModelHolder.getPropertyModel());
            } else {
                S id = propertyModelHolder.getPropertyModel().getPropertyAccessor().get(instance);
                if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                    id = propertyModelHolder.getIdGenerator().generate();
                    propertyModelHolder.getPropertyModel().getPropertyAccessor().set(instance, id);
                }
                encodeValue(writer, encoderContext, propertyModelHolder.getPropertyModel(), id);
            }
        }
    }

    protected <S> void encodeProperty(final BsonWriter writer, final T instance, final EncoderContext encoderContext,
                                      final PropertyModel<S> propertyModel) {
        if (propertyModel != null && propertyModel.isReadable()) {
            S propertyValue = propertyModel.getPropertyAccessor().get(instance);
            encodeValue(writer, encoderContext, propertyModel, propertyValue);
        }
    }

    @SuppressWarnings("unchecked")
    private void encodeEntity(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        if (areEquivalentTypes(value.getClass(), morphiaCodec.getClassModel().getType())) {
            writer.writeStartDocument();

            encodeIdProperty(writer, value, encoderContext, morphiaCodec.getClassModel().getIdPropertyModelHolder());

            if (morphiaCodec.getClassModel().useDiscriminator()) {
                writer.writeString(morphiaCodec.getClassModel().getDiscriminatorKey(), morphiaCodec.getClassModel().getDiscriminator());
            }

            for (PropertyModel<?> propertyModel : morphiaCodec.getClassModel().getPropertyModels()) {
                if (propertyModel.equals(morphiaCodec.getClassModel().getIdPropertyModel())) {
                    continue;
                }
                encodeProperty(writer, value, encoderContext, propertyModel);
            }
            writer.writeEndDocument();
        } else {
            morphiaCodec.getRegistry().get((Class<T>) value.getClass())
                        .encode(writer, value, encoderContext);
        }
    }

    private <S> void encodeValue(final BsonWriter writer, final EncoderContext encoderContext, final PropertyModel<S> propertyModel,
                                 final S propertyValue) {
        if (propertyModel.shouldSerialize(propertyValue)) {
            writer.writeName(propertyModel.getReadName());
            if (propertyValue == null) {
                writer.writeNull();
            } else {
                try {
                    encoderContext.encodeWithChildContext(propertyModel.getCachedCodec(), writer, propertyValue);
                } catch (CodecConfigurationException e) {
                    throw new CodecConfigurationException(String.format("Failed to encode '%s'. Encoding '%s' errored with: %s",
                        morphiaCodec.getClassModel().getName(), propertyModel.getReadName(), e.getMessage()), e);
                }
            }
        }
    }

    private void encodeWithLifecycle(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        Document document = new Document();
        morphiaCodec.getMappedClass().callLifecycleMethods(PrePersist.class, value, document, morphiaCodec.getMapper());

        final DocumentWriter documentWriter = new DocumentWriter(document);
        encodeEntity(documentWriter, value, encoderContext);
        document = documentWriter.getRoot();
        morphiaCodec.getMappedClass().callLifecycleMethods(PostPersist.class, value, document, morphiaCodec.getMapper());

        morphiaCodec.getRegistry().get(Document.class).encode(writer, document, encoderContext);
    }

}
