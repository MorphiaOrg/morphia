package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.mapping.DiscriminatorLookup;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.PropertyCodecRegistryImpl;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
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
public class MorphiaCodec<T> implements CollectibleCodec<T> {
    private final MappedField idField;
    private final Mapper mapper;
    private final EntityModel<T> entityModel;
    private final MappedClass mappedClass;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private final EntityEncoder<T> encoder = new EntityEncoder<>(this);
    private final EntityDecoder<T> decoder = new EntityDecoder<>(this);

    /**
     * Creates a new codec
     *
     * @param datastore              the datastore
     * @param mappedClass            the MappedClass backing this codec
     * @param propertyCodecProviders the codec provider for properties
     * @param registry               the codec registry for lookups
     * @param discriminatorLookup    the discriminator to type lookup
     */
    @SuppressWarnings("unchecked")
    public MorphiaCodec(final Datastore datastore, final MappedClass mappedClass,
                        final List<PropertyCodecProvider> propertyCodecProviders,
                        final DiscriminatorLookup discriminatorLookup, final CodecRegistry registry) {
        this.mappedClass = mappedClass;
        this.mapper = datastore.getMapper();
        this.discriminatorLookup = discriminatorLookup;

        this.entityModel = (EntityModel<T>) mappedClass.getEntityModel();
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.propertyCodecRegistry = new PropertyCodecRegistryImpl(this, registry, propertyCodecProviders);
        idField = mappedClass.getIdField();
        specializePropertyCodecs();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void specializePropertyCodecs() {
        EntityModel<T> entityModel = getEntityModel();
        for (FieldModel<?> fieldModel : entityModel.getFieldModels()) {
            Codec codec = fieldModel.getCodec() != null ? fieldModel.getCodec()
                                                        : propertyCodecRegistry.get(fieldModel.getTypeData());
            fieldModel.cachedCodec(codec);
        }
    }

    /**
     * @return the entity model backing this codec
     */
    public EntityModel<T> getEntityModel() {
        return entityModel;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return decoder.decode(reader, decoderContext);
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        encoder.encode(writer, value, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return getEntityModel().getType();
    }

    @Override
    public T generateIdIfAbsentFromDocument(final T entity) {
        if (!documentHasId(entity)) {
            idField.setFieldValue(entity, convert(new ObjectId(), idField.getType()));
        }
        return entity;
    }

    @Override
    public boolean documentHasId(final T entity) {
        return mappedClass.getIdField().getFieldValue(entity) != null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BsonValue getDocumentId(final T document) {
        final Object id = mappedClass.getIdField().getFieldValue(document);
        final DocumentWriter writer = new DocumentWriter();
        ((Codec) registry.get(id.getClass()))
            .encode(writer, id, EncoderContext.builder().build());
        Document doc = writer.getDocument();
        if (1 == 1) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        }

        return null;
    }

    /**
     * @return the MappedClass for this codec
     */
    public MappedClass getMappedClass() {
        return mappedClass;
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
