package dev.morphia.mapping.codec.pojo;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IdGenerator;
import org.bson.codecs.ObjectIdGenerator;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.Map;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * @morphia.internal
 * @since 2.0
 */
class EntityEncoder implements org.bson.codecs.Encoder<Object> {
    public static final ObjectIdGenerator OBJECT_ID_GENERATOR = new ObjectIdGenerator();
    private final MorphiaCodec morphiaCodec;
    private IdGenerator idGenerator;

    protected EntityEncoder(MorphiaCodec morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        encodeEntity(writer, value, encoderContext);
    }

    @SuppressWarnings("unchecked")
    protected void encodeEntity(BsonWriter writer, Object value, EncoderContext encoderContext) {
        EntityModel model = morphiaCodec.getEntityModel();
        if (areEquivalentTypes(value.getClass(), model.getType())) {
            document(writer, () -> {

                PropertyModel idModel = model.getIdProperty();
                encodeIdProperty(writer, value, encoderContext, idModel);

                if (model.useDiscriminator()) {
                    writer.writeString(model.getDiscriminatorKey(),
                        model.getDiscriminator());
                }

                for (PropertyModel propertyModel : model.getProperties()) {
                    if (propertyModel.equals(idModel)) {
                        continue;
                    }
                    encodeProperty(writer, value, encoderContext, propertyModel);
                }
            });
        } else {
            morphiaCodec.getRegistry()
                        .get((Class<? super Object>) value.getClass())
                        .encode(writer, value, encoderContext);
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

    protected MorphiaCodec getMorphiaCodec() {
        return morphiaCodec;
    }

    private void encodeIdProperty(BsonWriter writer, Object instance, EncoderContext encoderContext,
                                  PropertyModel idModel) {
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
                                PropertyModel model) {
        if (model != null) {
            Object value = model.getAccessor().get(instance);
            encodeValue(writer, encoderContext, model, value);
        }
    }

    private void encodeValue(BsonWriter writer, EncoderContext encoderContext, PropertyModel model,
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

    private IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            PropertyModel idModel = morphiaCodec.getEntityModel().getIdProperty();
            if (idModel.getNormalizedType().isAssignableFrom(ObjectId.class)) {
                idGenerator = OBJECT_ID_GENERATOR;
            }
        }

        return idGenerator;
    }

}
