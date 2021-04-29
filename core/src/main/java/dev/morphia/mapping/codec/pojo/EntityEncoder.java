package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;
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
        EntityModel model = morphiaCodec.getEntityModel();
        if (areEquivalentTypes(value.getClass(), model.getType())) {
            document(writer, () -> {

                PropertyModel idModel = model.getIdProperty();
                encodeIdProperty(writer, value, encoderContext, idModel);

                if (model.useDiscriminator()) {
                    encodeDiscriminator(writer, model);
                }

                for (PropertyModel propertyModel : model.getProperties()) {
                    if (propertyModel.equals(idModel)) {
                        continue;
                    }
                    encodeValue(writer, encoderContext, propertyModel, propertyModel.getAccessor().get(value));
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

    protected void encodeDiscriminator(BsonWriter writer, EntityModel model) {
        writer.writeString(model.getDiscriminatorKey(), model.getDiscriminator());
    }

    protected MorphiaCodec getMorphiaCodec() {
        return morphiaCodec;
    }

    private <S, V> boolean areEquivalentTypes(Class<S> t1, Class<V> t2) {
        return t1.equals(t2)
               || Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)
               || Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2);
    }

    protected void encodeIdProperty(BsonWriter writer, Object instance, EncoderContext encoderContext, @Nullable PropertyModel idModel) {
        if (idModel != null) {
            IdGenerator generator = getIdGenerator();
            if (generator == null) {
                encodeValue(writer, encoderContext, idModel, idModel.getAccessor().get(instance));
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

    protected void encodeValue(BsonWriter writer, EncoderContext encoderContext, PropertyModel model, @Nullable Object value) {
        if (model.shouldSerialize(value)) {
            writeValue(writer, encoderContext, model, value);
        }
    }

    protected void writeValue(BsonWriter writer, EncoderContext encoderContext, PropertyModel model, @Nullable Object value) {
        writer.writeName(model.getMappedName());
        if (value == null) {
            writer.writeNull();
        } else {
            Codec<? super Object> cachedCodec = model.getCachedCodec();
            encoderContext.encodeWithChildContext(cachedCodec, writer, value);
        }
    }

    @Nullable
    private IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            PropertyModel idModel = morphiaCodec.getEntityModel().getIdProperty();
            if (idModel != null && idModel.getNormalizedType().isAssignableFrom(ObjectId.class)) {
                idGenerator = OBJECT_ID_GENERATOR;
            }
        }

        return idGenerator;
    }
}
