package dev.morphia.mapping.codec.pojo;

import java.util.Collection;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IdGenerator;
import org.bson.codecs.ObjectIdGenerator;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static java.lang.String.format;

/**
 * @param <T> the entity type
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class EntityEncoder<T> implements org.bson.codecs.Encoder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityEncoder.class);

    public static final ObjectIdGenerator OBJECT_ID_GENERATOR = new ObjectIdGenerator();
    private final MorphiaCodec<T> morphiaCodec;
    private IdGenerator idGenerator;

    protected EntityEncoder(MorphiaCodec<T> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        EntityModel model = morphiaCodec.getEntityModel();
        if (areEquivalentTypes(value.getClass(), model.getType())) {
            LOG.debug(format("Encoding document using codec for %s'", morphiaCodec.getEntityModel().getType().getName()));

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
                    encodeProperty(writer, propertyModel, value, encoderContext);
                }
            });
        } else {
            morphiaCodec.getRegistry()
                    .get((Class) value.getClass())
                    .encode(writer, value, encoderContext);
        }
    }

    /**
     * Encodes a property on the model
     *
     * @param writer         the writer
     * @param propertyModel  the property to encode
     * @param value          the value of the property
     * @param encoderContext the context
     * @since 2.3
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public void encodeProperty(BsonWriter writer, PropertyModel propertyModel, Object value, EncoderContext encoderContext) {
        encodeValue(writer, encoderContext, propertyModel, propertyModel.getAccessor().get(value));
    }

    @Override
    public Class<T> getEncoderClass() {
        return morphiaCodec.getEncoderClass();
    }

    protected <S, V> boolean areEquivalentTypes(Class<S> t1, Class<V> t2) {
        return t1.equals(t2)
                || Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)
                || Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2);
    }

    protected void encodeDiscriminator(BsonWriter writer, EntityModel model) {
        writer.writeString(model.discriminatorKey(), model.discriminator());
    }

    protected void encodeIdProperty(BsonWriter writer, Object instance, EncoderContext encoderContext, @Nullable PropertyModel idModel) {
        if (idModel != null) {
            Object id = idModel.getAccessor().get(instance);
            if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                IdGenerator generator = getIdGenerator();
                if (generator != null) {
                    id = generator.generate();
                    idModel.getAccessor().set(instance, id);
                }
            }
            encodeValue(writer, encoderContext, idModel, id);
        }
    }

    protected void encodeValue(BsonWriter writer, EncoderContext encoderContext, PropertyModel model, @Nullable Object value) {
        if (model.shouldSerialize(value)) {
            writeValue(writer, encoderContext, model, value);
        }
    }

    @Nullable
    protected IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            PropertyModel idModel = morphiaCodec.getEntityModel().getIdProperty();
            if (idModel != null && idModel.getNormalizedType().isAssignableFrom(ObjectId.class)) {
                idGenerator = OBJECT_ID_GENERATOR;
            }
        }

        return idGenerator;
    }

    protected MorphiaCodec<T> getMorphiaCodec() {
        return morphiaCodec;
    }

    protected void writeValue(BsonWriter writer, EncoderContext encoderContext, PropertyModel model, @Nullable Object value) {
        writer.writeName(model.getMappedName());
        if (value == null) {
            writer.writeNull();
        } else {
            Codec<? super Object> codec = (Codec<? super Object>) model.getCodec();
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }
}
