package dev.morphia.mapping.codec.pojo;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DiscriminatorLookup;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.mapping.codec.Conversions.convert;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * the codec used by Morphia
 *
 * @param <T> the entity type
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class MorphiaCodec<T> implements CollectibleCodec<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaCodec.class);

    private final PropertyModel idProperty;
    private final EntityModel entityModel;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private EntityEncoder<T> encoder;
    private EntityDecoder<T> decoder;
    private MorphiaDatastore datastore;

    /**
     * Creates a new codec
     *
     * @param datastore
     * @param model                  the model backing this codec
     * @param propertyCodecProviders the codec provider for properties
     * @param discriminatorLookup    the discriminator to type lookup
     * @param registry               the codec registry for lookups
     */
    public MorphiaCodec(MorphiaDatastore datastore, EntityModel model,
            List<PropertyCodecProvider> propertyCodecProviders,
            DiscriminatorLookup discriminatorLookup, CodecRegistry registry) {
        this.datastore = datastore;
        this.discriminatorLookup = discriminatorLookup;

        this.entityModel = model;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.propertyCodecRegistry = new PropertyCodecRegistryImpl(this, registry, propertyCodecProviders);
        idProperty = model.getIdProperty();
        specializePropertyCodecs();
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        return getDecoder().decode(reader, decoderContext);
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        getEncoder().encode(writer, value, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return (Class<T>) getEntityModel().getType();
    }

    @Override
    public Object generateIdIfAbsentFromDocument(Object entity) {
        if (!documentHasId(entity)) {
            if (idProperty != null) {
                if (ObjectId.class.equals(idProperty.getType()) || String.class.equals(idProperty.getType())) {
                    idProperty.setValue(entity, convert(new ObjectId(), idProperty.getType()));
                } else {
                    LOG.warn(Sofia.noIdAndNotObjectId(entity.getClass().getName()));
                }
            }
        }
        return entity;
    }

    @Override
    public boolean documentHasId(Object entity) {
        if (idProperty == null) {
            throw new MappingException(Sofia.idRequired(entity.getClass().getName()));
        }
        return idProperty.getValue(entity) != null;
    }

    @Override
    public BsonValue getDocumentId(Object document) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the datastore
     * @since 2.3
     */
    public MorphiaDatastore getDatastore() {
        return datastore;
    }

    /**
     * @return the DiscriminatorLookup
     */
    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    /**
     * @return the encoder
     * @since 2.3
     */
    public EntityEncoder<T> getEncoder() {
        if (encoder == null) {
            encoder = new EntityEncoder<>(this);
        }
        return encoder;
    }

    /**
     * Sets the encoder
     *
     * @param encoder the encoder
     * @return this
     */
    public MorphiaCodec<T> setEncoder(EntityEncoder<T> encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * @return the entity model backing this codec
     */
    public EntityModel getEntityModel() {
        return entityModel;
    }

    /**
     * @return the codec registry
     */
    public CodecRegistry getRegistry() {
        return registry;
    }

    /**
     * @return the decoder
     */
    protected EntityDecoder<T> getDecoder() {
        if (decoder == null) {
            decoder = new EntityDecoder<>(this);
        }
        return decoder;
    }

    /**
     * Sets the decoder
     *
     * @param decoder the decoder
     * @return
     */
    public MorphiaCodec<T> setDecoder(EntityDecoder<T> decoder) {
        this.decoder = decoder;
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void specializePropertyCodecs() {
        EntityModel entityModel = getEntityModel();
        for (PropertyModel propertyModel : entityModel.getProperties()) {
            Codec<?> specializeCodec = propertyModel.specializeCodec(getDatastore());
            Codec codec = specializeCodec != null ? specializeCodec
                    : propertyCodecRegistry.get(propertyModel.getTypeData());
            if (codec != null) {
                propertyModel.codec(codec);
            }
        }
    }

}
