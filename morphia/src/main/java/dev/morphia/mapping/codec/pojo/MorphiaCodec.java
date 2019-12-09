package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.PropertyCodecRegistryImpl;
import org.bson.codecs.pojo.PropertyModel;
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
public class MorphiaCodec<T> extends PojoCodec<T> implements CollectibleCodec<T> {
    protected final MappedField idField;
    private final Mapper mapper;
    private final EntityModel<T> entityModel;
    private final MappedClass mappedClass;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private final Encoder<T> encoder = new Encoder<>(this);
    private final Decoder<T> decoder = new Decoder<>(this);

    /**
     * Creates a new codec
     *
     * @param datastore              the datastore
     * @param mappedClass            the MappedClass backing this codec
     * @param propertyCodecProviders the codec provider for properties
     * @param registry               the codec registry for lookups
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
        ClassModel<T> classModel = getClassModel();
        for (PropertyModel<?> propertyModel : classModel.getPropertyModels()) {
            Codec codec = propertyModel.getCodec() != null ? propertyModel.getCodec()
                                                           : propertyCodecRegistry.get(propertyModel.getTypeData());
            propertyModel.cachedCodec(codec);
        }
    }

    @Override
    public EntityModel<T> getClassModel() {
        return entityModel;
    }

    public MappedClass getMappedClass() {
        return mappedClass;
    }

    DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    CodecRegistry getRegistry() {
        return registry;
    }

    PropertyCodecRegistry getPropertyCodecRegistry() {
        return propertyCodecRegistry;
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
        return getClassModel().getType();
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
        return writer.getRoot();
    }

    /**
     * @return the mapper being used
     */
    public Mapper getMapper() {
        return mapper;
    }

}
