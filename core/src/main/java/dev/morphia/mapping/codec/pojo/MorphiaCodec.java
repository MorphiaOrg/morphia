package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.mapping.DiscriminatorLookup;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.PropertyCodecRegistryImpl;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.types.ObjectId;

import java.util.List;

import static dev.morphia.mapping.codec.Conversions.convert;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * the codec used by Morphia
 *
 * @param <T> the entity type
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class MorphiaCodec<T> implements CollectibleCodec<T> {
    private final PropertyModel idProperty;
    private final Mapper mapper;
    private final EntityModel entityModel;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private EntityEncoder encoder;
    private EntityDecoder decoder;

    /**
     * Creates a new codec
     *
     * @param datastore              the datastore
     * @param model                  the model backing this codec
     * @param propertyCodecProviders the codec provider for properties
     * @param registry               the codec registry for lookups
     * @param discriminatorLookup    the discriminator to type lookup
     */
    public MorphiaCodec(Datastore datastore, EntityModel model,
                        List<PropertyCodecProvider> propertyCodecProviders,
                        DiscriminatorLookup discriminatorLookup, CodecRegistry registry) {
        this.mapper = datastore.getMapper();
        this.discriminatorLookup = discriminatorLookup;

        this.entityModel = model;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.propertyCodecRegistry = new PropertyCodecRegistryImpl(this, registry, propertyCodecProviders);
        idProperty = model.getIdProperty();
        specializePropertyCodecs();
        encoder = new EntityEncoder(this);
        decoder = new EntityDecoder(this);
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        return (T) getDecoder().decode(reader, decoderContext);
    }

    @Override
    public boolean documentHasId(Object entity) {
        PropertyModel idField = entityModel.getIdProperty();
        if (idField == null) {
            throw new MappingException(Sofia.idRequired(entity.getClass().getName()));
        }
        return idField.getValue(entity) != null;
    }

    /**
     * @return the entity model backing this codec
     */
    public EntityModel getEntityModel() {
        return entityModel;
    }

    /**
     * @return the encoder
     */
    public EntityEncoder getEncoder() {
        return encoder;
    }

    /**
     * Sets the encoder
     *
     * @param encoder the encoder
     * @return this
     */
    public MorphiaCodec<T> setEncoder(EntityEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * @return the decoder
     */
    protected EntityDecoder getDecoder() {
        return decoder;
    }

    /**
     * Sets the decoder
     *
     * @param decoder the decoder
     */
    public void setDecoder(EntityDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        encoder.encode(writer, value, encoderContext);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getEncoderClass() {
        return getEntityModel().getType();
    }

    @Override
    public Object generateIdIfAbsentFromDocument(Object entity) {
        if (!documentHasId(entity)) {
            idProperty.setValue(entity, convert(new ObjectId(), idProperty.getType()));
        }
        return entity;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void specializePropertyCodecs() {
        EntityModel entityModel = getEntityModel();
        for (PropertyModel propertyModel : entityModel.getProperties()) {
            Codec codec = propertyModel.getCodec() != null ? propertyModel.getCodec()
                                                           : propertyCodecRegistry.get(propertyModel.getTypeData());
            propertyModel.cachedCodec(codec);
        }
    }

    @Override
    public BsonValue getDocumentId(Object document) {
        throw new UnsupportedOperationException("is this even necessary?");
/*
        final Object id = mappedClass.getIdField().getFieldValue(document);
        final DocumentWriter writer = new DocumentWriter();
        ((Codec) registry.get(id.getClass()))
            .encode(writer, id, EncoderContext.builder().build());
        Document doc = writer.getDocument();

        return null;
*/
    }

    /**
     * @return the mapper being used
     */
    public Mapper getMapper() {
        return mapper;
    }

    DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    CodecRegistry getRegistry() {
        return registry;
    }

}
