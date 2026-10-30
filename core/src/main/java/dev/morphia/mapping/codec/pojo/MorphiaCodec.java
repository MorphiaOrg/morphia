package dev.morphia.mapping.codec.pojo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DiscriminatorLookup;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.DecodeSession;
import dev.morphia.mapping.codec.PropertyCodecRegistryImpl;
import dev.morphia.mapping.codec.references.ReferenceCodec;
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
    private final Conversions conversions;
    private final CodecPlan plan;
    // discriminator -> codec for the other types a document read through this codec can name; package-visible for tests
    final Map<String, Codec<?>> discriminatorCodecs = new ConcurrentHashMap<>();
    // subtype -> codec, for values of a subtype encoded through this codec
    private final Map<Class<?>, Codec<?>> subtypeCodecs = new ConcurrentHashMap<>();
    private EntityEncoder<T> encoder;
    private EntityDecoder<T> decoder;
    // used in place of the plain encoder and decoder once a global listener claims their events; built when first needed
    private volatile EntityEncoder<T> lifecycleEncoder;
    private volatile EntityDecoder<T> lifecycleDecoder;
    private MorphiaDatastore datastore;

    /**
     * Creates a new codec
     *
     * @param datastore
     * @param model                  the model backing this codec
     * @param propertyCodecProviders the codec provider for properties
     * @param discriminatorLookup    the discriminator to type lookup
     * @param registry               the codec registry for lookups
     * @param conversions            the Conversions instance to use
     */
    public MorphiaCodec(MorphiaDatastore datastore, EntityModel model,
            List<PropertyCodecProvider> propertyCodecProviders,
            DiscriminatorLookup discriminatorLookup, CodecRegistry registry,
            Conversions conversions) {
        this(datastore, model, propertyCodecProviders, discriminatorLookup, registry, conversions, null);
    }

    /**
     * Creates a codec that encodes and decodes with an existing plan, typically another codec's for the same model, so
     * the property codecs aren't resolved again.
     *
     * @param datastore
     * @param model                  the model backing this codec
     * @param propertyCodecProviders the codec provider for properties
     * @param discriminatorLookup    the discriminator to type lookup
     * @param registry               the codec registry for lookups
     * @param conversions            the Conversions instance to use
     * @param plan                   the plan to use, or null to build one for this codec
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaCodec(MorphiaDatastore datastore, EntityModel model,
            List<PropertyCodecProvider> propertyCodecProviders,
            DiscriminatorLookup discriminatorLookup, CodecRegistry registry,
            Conversions conversions, @Nullable CodecPlan plan) {
        this.datastore = datastore;
        this.discriminatorLookup = discriminatorLookup;
        this.conversions = conversions;

        this.entityModel = model;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.propertyCodecRegistry = new PropertyCodecRegistryImpl(this, registry, propertyCodecProviders);
        idProperty = model.getIdProperty();
        if (plan != null && plan.model() != model) {
            throw new IllegalArgumentException("The plan is for %s, not %s".formatted(plan.model().getType().getName(),
                    model.getType().getName()));
        }
        this.plan = plan != null ? plan : new CodecPlan(model, propertyCodecs(model));
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        EntityDecoder<T> decoder = decoder();
        if (DecodeSession.current() != null) {
            return decoder.decode(reader, decoderContext);
        }
        return DecodeSession.forConfig(datastore.getMapper().getConfig())
                .decoding(() -> decoder.decode(reader, decoderContext));
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        encoder().encode(writer, value, encoderContext);
    }

    /**
     * The encoder to use now. A codec built before a global listener for its events was added still runs that
     * listener: the plain encoder hands over to a lifecycle one. Other encoders are left as they are.
     */
    private EntityEncoder<T> encoder() {
        EntityEncoder<T> encoder = getEncoder();
        if (encoder.getClass() == EntityEncoder.class && !datastore.getMapper().getListeners().isEmpty()
                && LifecycleEncoder.isNeeded(entityModel, datastore.getMapper())) {
            EntityEncoder<T> lifecycle = lifecycleEncoder;
            if (lifecycle == null) {
                lifecycle = new LifecycleEncoder<>(this);
                lifecycleEncoder = lifecycle;
            }
            return lifecycle;
        }
        return encoder;
    }

    /**
     * The decoder to use now; see {@link #encoder()}.
     */
    private EntityDecoder<T> decoder() {
        EntityDecoder<T> decoder = getDecoder();
        if (decoder.getClass() == EntityDecoder.class && !datastore.getMapper().getListeners().isEmpty()
                && LifecycleDecoder.isNeeded(entityModel, datastore.getMapper())) {
            EntityDecoder<T> lifecycle = lifecycleDecoder;
            if (lifecycle == null) {
                lifecycle = new LifecycleDecoder<>(this);
                lifecycleDecoder = lifecycle;
            }
            return lifecycle;
        }
        return decoder;
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
                    idProperty.setValue(entity, conversions.convert(new ObjectId(), idProperty.getType()));
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
     * @return the Conversions instance
     */
    public Conversions getConversions() {
        return conversions;
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
     * @param discriminator a discriminator value read from a document
     * @return the codec for the type it names: this codec when it names this codec's own type, which is the usual
     *         case, otherwise the registry's codec for that type, looked up once per discriminator
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressWarnings("unchecked")
    public Codec<? extends T> codecForDiscriminator(String discriminator) {
        if (discriminator.equals(entityModel.discriminator())) {
            return this;
        }
        Codec<?> codec = discriminatorCodecs.get(discriminator);
        if (codec == null) {
            // not computeIfAbsent: building the codec can look up other codecs, which may land back here
            codec = registry.get(discriminatorLookup.lookup(discriminator));
            Codec<?> existing = discriminatorCodecs.putIfAbsent(discriminator, codec);
            if (existing != null) {
                codec = existing;
            }
        }
        return (Codec<? extends T>) codec;
    }

    /**
     * @param type the class of a value being encoded through this codec, a subtype of its own type
     * @return the codec for that class, looked up once per class
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressWarnings("unchecked")
    public <S> Codec<S> codecForSubtype(Class<S> type) {
        Codec<?> codec = subtypeCodecs.get(type);
        if (codec == null) {
            // not computeIfAbsent: building the codec can look up other codecs, which may land back here
            codec = registry.get(type);
            Codec<?> existing = subtypeCodecs.putIfAbsent(type, codec);
            if (existing != null) {
                codec = existing;
            }
        }
        return (Codec<S>) codec;
    }

    /**
     * @return the plan this codec encodes and decodes with
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public CodecPlan getPlan() {
        return plan;
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

    /**
     * Resolves each property's codec against this codec's datastore. They live in the plan, never on the shared
     * {@code PropertyModel}, so datastores and sessions sharing a mapper each get their own: a {@code ReferenceCodec}
     * fetches through the datastore it was built for.
     */
    private Codec<?>[] propertyCodecs(EntityModel model) {
        List<PropertyModel> properties = model.getProperties();
        Codec<?>[] codecs = new Codec<?>[properties.size()];
        for (int slot = 0; slot < codecs.length; slot++) {
            PropertyModel property = properties.get(slot);
            if (property.hasAnnotation(Reference.class)) {
                codecs[slot] = new ReferenceCodec(datastore, property);
            } else if (property.getTypeData().getTypeParameters().isEmpty()) {
                codecs[slot] = datastore.getCodecRegistry().get(property.getType());
            } else {
                codecs[slot] = propertyCodecRegistry.get(property.getTypeData());
            }
        }
        return codecs;
    }

}
