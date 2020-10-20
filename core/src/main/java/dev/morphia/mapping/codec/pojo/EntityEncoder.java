package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IdGenerator;
import org.bson.codecs.ObjectIdGenerator;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.Map;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * @since 2.0
 */
class EntityEncoder implements org.bson.codecs.Encoder<Object> {
    public static final ObjectIdGenerator OBJECT_ID_GENERATOR = new ObjectIdGenerator();
    private final MorphiaCodec morphiaCodec;
    private IdGenerator idGenerator;

    EntityEncoder(MorphiaCodec morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        EntityModel model = morphiaCodec.getEntityModel();
        if (model.hasLifecycle(PostPersist.class)
            || model.hasLifecycle(PrePersist.class)
            || morphiaCodec.getMapper().hasInterceptors()) {

            encodeWithLifecycle(writer, value, encoderContext);
        } else {
            encodeEntity(writer, value, encoderContext);
        }
    }

    @Override
    public Class<Object> getEncoderClass() {
        return morphiaCodec.getEncoderClass();
    }

    private <S, V> boolean areEquivalentTypes(Class<S> t1, Class<V> t2) {
        return t1.equals(t2)
               || Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)
               || Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2);
    }

    @SuppressWarnings("unchecked")
    private void encodeEntity(BsonWriter writer, Object value, EncoderContext encoderContext) {
        if (areEquivalentTypes(value.getClass(), morphiaCodec.getEntityModel().getType())) {
            document(writer, () -> {

                FieldModel idModel = morphiaCodec.getEntityModel().getIdField();
                encodeIdProperty(writer, value, encoderContext, idModel);

                if (morphiaCodec.getEntityModel().useDiscriminator()) {
                    writer.writeString(morphiaCodec.getEntityModel().getDiscriminatorKey(),
                        morphiaCodec.getEntityModel().getDiscriminator());
                }

                for (FieldModel fieldModel : morphiaCodec.getEntityModel().getFields()) {
                    if (fieldModel.equals(idModel)) {
                        continue;
                    }
                    encodeProperty(writer, value, encoderContext, fieldModel);
                }
            });
        } else {
            morphiaCodec.getRegistry().get((Class<? super Object>) value.getClass())
                        .encode(writer, value, encoderContext);
        }
    }

    private void encodeIdProperty(BsonWriter writer, Object instance, EncoderContext encoderContext,
                                  FieldModel idModel) {
        if (idModel != null) {
            IdGenerator generator = getIdGenerator();
            if (generator == null) {
                encodeProperty(writer, instance, encoderContext, idModel);
            } else {
                Object id = idModel.getAccessor().get(instance);
                if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                    id = generator.generate();
                    idModel.getAccessor().set(instance, id);
                }
                encodeValue(writer, encoderContext, idModel, id);
            }
        }
    }

    private void encodeProperty(BsonWriter writer, Object instance, EncoderContext encoderContext,
                                FieldModel model) {
        if (model != null) {
            Object value = model.getAccessor().get(instance);
            encodeValue(writer, encoderContext, model, value);
        }
    }

    private void encodeValue(BsonWriter writer, EncoderContext encoderContext, FieldModel model,
                             Object propertyValue) {
        if (model.shouldSerialize(propertyValue)) {
            writer.writeName(model.getMappedName());
            if (propertyValue == null) {
                writer.writeNull();
            } else {
                Codec<? super Object> cachedCodec = model.getCachedCodec();
                encoderContext.encodeWithChildContext(cachedCodec, writer, propertyValue);
            }
        }
    }

    private void encodeWithLifecycle(BsonWriter writer, Object value, EncoderContext encoderContext) {
        Document document = new Document();
        morphiaCodec.getEntityModel().callLifecycleMethods(PrePersist.class, value, document, morphiaCodec.getMapper());

        final DocumentWriter documentWriter = new DocumentWriter(document);
        encodeEntity(documentWriter, value, encoderContext);
        document = documentWriter.getDocument();
        morphiaCodec.getEntityModel().callLifecycleMethods(PostPersist.class, value, document, morphiaCodec.getMapper());

        morphiaCodec.getRegistry().get(Document.class).encode(writer, document, encoderContext);
    }

    private IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            FieldModel idModel = morphiaCodec.getEntityModel().getIdField();
            if (idModel.getNormalizedType().isAssignableFrom(ObjectId.class)) {
                idGenerator = OBJECT_ID_GENERATOR;
            }
        }

        return idGenerator;
    }

}
