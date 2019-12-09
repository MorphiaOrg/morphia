package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.reader.DocumentReader;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.DiscriminatorLookup;
import org.bson.codecs.pojo.IdPropertyModelHolder;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.PropertyCodecRegistryImpl;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static dev.morphia.mapping.codec.Conversions.convert;
import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * the codec used by Morphia
 *
 * @param <T> the entity type
 */
public class MorphiaCodec<T> extends PojoCodec<T> implements CollectibleCodec<T> {
    protected final MappedField idField;
    private final Mapper mapper;
    private final Datastore datastore;
    private final EntityModel<T> entityModel;
    private final MappedClass mappedClass;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache;
    private final boolean specialized;
    private final Encoder encoder = new Encoder();
    private final Decoder decoder = new Decoder();

    /**
     * Creates a new codec
     *
     * @param datastore              the datastore
     * @param propertyCodecProviders the codec provider for properties
     * @param discriminatorLookup    the discriminator lookup
     * @param registry               the codec registry for lookups
     */
    @SuppressWarnings("unchecked")
    public MorphiaCodec(final Datastore datastore, final MappedClass mappedClass,
                        final List<PropertyCodecProvider> propertyCodecProviders,
                        final DiscriminatorLookup discriminatorLookup, final CodecRegistry registry) {
        this.datastore = datastore;
        this.mappedClass = mappedClass;
        this.mapper = datastore.getMapper();
        this.discriminatorLookup = discriminatorLookup;
        this.codecCache = new ConcurrentHashMap<>();

        this.entityModel = (EntityModel<T>) mappedClass.getEntityModel();
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.propertyCodecRegistry = new PropertyCodecRegistryImpl(this, registry, propertyCodecProviders);
        this.specialized = true;
        idField = mappedClass.getIdField();
        specialize();
    }

    /**
     * Creates a new codec
     *
     * @param datastore             the datastore
     * @param propertyCodecRegistry the codec registry for properties
     * @param discriminatorLookup   the discriminator lookup
     * @param codecCache            the cache of codecs
     * @param specialized           has this codec been specialized for a particular instance/field
     * @param registry              the codec registry for lookups
     */
    @SuppressWarnings("unchecked")
    public MorphiaCodec(final Datastore datastore, final MappedClass mappedClass, final PropertyCodecRegistry propertyCodecRegistry,
                        final DiscriminatorLookup discriminatorLookup, final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache,
                        final boolean specialized, final CodecRegistry registry) {
        this.mapper = datastore.getMapper();
        this.datastore = datastore;
        this.mappedClass = mappedClass;
        this.discriminatorLookup = discriminatorLookup;
        this.codecCache = codecCache;
        this.propertyCodecRegistry = propertyCodecRegistry;
        this.specialized = specialized;

        this.entityModel = (EntityModel<T>) mappedClass.getEntityModel();
        this.registry = fromRegistries(fromCodecs(this), registry);
        idField = mappedClass.getIdField();
        specialize();
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return decoder.decode(reader, decoderContext);
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
        ((Codec) getRegistry().get(id.getClass()))
            .encode(writer, id, EncoderContext.builder().build());
        return writer.getRoot();
    }

    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        encoder.encode(writer, value, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return getClassModel().getType();
    }

    /**
     * @return the mapper being used
     */
    public Mapper getMapper() {
        return mapper;
    }

    public CodecRegistry getRegistry() {
        return registry;
    }

    public PropertyCodecRegistry getPropertyCodecRegistry() {
        return propertyCodecRegistry;
    }

    public DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    public ConcurrentMap<ClassModel<?>, Codec<?>> getCodecCache() {
        return codecCache;
    }

    public boolean isSpecialized() {
        return specialized;
    }

    @Override
    public EntityModel<T> getClassModel() {
        return entityModel;
    }

    public MappedClass getMappedClass() {
        return mappedClass;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void specialize() {
        ClassModel<T> classModel = getClassModel();
        getCodecCache().put(classModel, this);
        for (PropertyModel<?> propertyModel : classModel.getPropertyModels()) {
            Codec codec = propertyModel.getCodec() != null ? propertyModel.getCodec()
                                                           : getPropertyCodecRegistry().get(propertyModel.getTypeData());
            propertyModel.cachedCodec(codec);
        }
    }

    protected PojoCodec<T> getSpecializedCodec(final Datastore datastore) {
        return new SpecializedMorphiaCodec<>(this, mappedClass, entityModel, datastore);
    }

    class Decoder implements org.bson.codecs.Decoder<T> {
        @Override
        public T decode(final BsonReader reader, final DecoderContext decoderContext) {
            T entity;
            if (mappedClass.hasLifecycle(PreLoad.class) || mappedClass.hasLifecycle(PostLoad.class) || getMapper().hasInterceptors()) {
                entity = decodeWithLifecycle(reader, decoderContext);
            } else {
                EntityModel<T> classModel = getClassModel();
                if (decoderContext.hasCheckedDiscriminator()) {
                    if (!isSpecialized()) {
                        throw new CodecConfigurationException(format("%s contains generic types that have not been specialised.%n"
                                                                     + "Top level classes with generic types are not supported by the "
                                                                     + "PojoCodec.", classModel.getName()));
                    }
                    MorphiaInstanceCreator<T> instanceCreator = classModel.getInstanceCreator();
                    decodeProperties(reader, decoderContext, instanceCreator);
                    return instanceCreator.getInstance();
                } else {
                    entity =  getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(), getRegistry(),
                        getDiscriminatorLookup(), MorphiaCodec.this)
                                  .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
                }
            }

            return entity;
        }

        private T decodeWithLifecycle(final BsonReader reader, final DecoderContext decoderContext) {
            final T entity;
            final MorphiaInstanceCreator<T> instanceCreator = getClassModel().getInstanceCreator();
            entity = instanceCreator.getInstance();

            Document document = getRegistry().get(Document.class).decode(reader, decoderContext);
            mappedClass.callLifecycleMethods(PreLoad.class, entity, document, getMapper());

            decodeProperties(new DocumentReader(document), decoderContext, instanceCreator);

            mappedClass.callLifecycleMethods(PostLoad.class, entity, document, getMapper());
            return entity;
        }

        protected void decodeProperties(final BsonReader reader, final DecoderContext decoderContext,
                                        final MorphiaInstanceCreator<T> instanceCreator) {
            reader.readStartDocument();
            EntityModel<T> classModel = getClassModel();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                    reader.readString();
                } else {
                    decodePropertyModel(reader, decoderContext, instanceCreator, classModel.getPropertyModel(name));
                }
            }
            reader.readEndDocument();
        }

        @SuppressWarnings("unchecked")
        protected <S> void decodePropertyModel(final BsonReader reader, final DecoderContext decoderContext,
                                               final InstanceCreator<T> instanceCreator, final PropertyModel<S> propertyModel) {

            if (propertyModel != null) {
                final BsonReaderMark mark = reader.getMark();
                try {
                    S value = null;
                    if (reader.getCurrentBsonType() == BsonType.NULL) {
                        reader.readNull();
                    } else {
                        value = decoderContext.decodeWithChildContext(propertyModel.getCachedCodec(), reader);
                    }
                    if (propertyModel.isWritable()) {
                        instanceCreator.set(value, propertyModel);
                    }
                } catch (BsonInvalidOperationException e) {
                    mark.reset();
                    final Object value = getMapper().getCodecRegistry().get(Object.class).decode(reader, decoderContext);
                    instanceCreator.set((S) convert(value, propertyModel.getTypeData().getType()), propertyModel);
                }
            } else {
                reader.skipValue();
            }
        }

        @SuppressWarnings("unchecked")
        protected Codec<T> getCodecFromDocument(final BsonReader reader, final boolean useDiscriminator, final String discriminatorKey,
                                                final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                                final Codec<T> defaultCodec) {
            Codec<T> codec = null;
            if (useDiscriminator) {
                BsonReaderMark mark = reader.getMark();
                try {
                    reader.readStartDocument();
                    while (codec == null && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        if (discriminatorKey.equals(reader.readName())) {
                            codec = (Codec<T>) registry.get(discriminatorLookup.lookup(reader.readString()));
                        } else {
                            reader.skipValue();
                        }
                    }
                } catch (Exception e) {
                    throw new CodecConfigurationException(format("Failed to decode '%s'. Decoding errored with: %s",
                        getClassModel().getName(), e.getMessage()), e);
                } finally {
                    mark.reset();
                }
            }
            return codec != null ? codec : defaultCodec;
        }

    }

    class Encoder implements org.bson.codecs.Encoder<T> {
        @Override
        public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
            if (mappedClass.hasLifecycle(PostPersist.class)
                || mappedClass.hasLifecycle(PrePersist.class)
                || getMapper().hasInterceptors()) {

                encodeWithLifecycle(writer, value, encoderContext);
            } else {
                encodeEntity(writer, value, encoderContext);
            }
        }

        @SuppressWarnings("unchecked")
        private void encodeEntity(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
            if (areEquivalentTypes(value.getClass(), getClassModel().getType())) {
                writer.writeStartDocument();

                encodeIdProperty(writer, value, encoderContext, getClassModel().getIdPropertyModelHolder());

                if (getClassModel().useDiscriminator()) {
                    writer.writeString(getClassModel().getDiscriminatorKey(), getClassModel().getDiscriminator());
                }

                for (PropertyModel<?> propertyModel : getClassModel().getPropertyModels()) {
                    if (propertyModel.equals(getClassModel().getIdPropertyModel())) {
                        continue;
                    }
                    encodeProperty(writer, value, encoderContext, propertyModel);
                }
                writer.writeEndDocument();
            } else {
                specializeCodec((Class<T>) value.getClass()).encode(writer, value, encoderContext);
            }
        }

        private void encodeWithLifecycle(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
            Document document = new Document();
            mappedClass.callLifecycleMethods(PrePersist.class, value, document, getMapper());

            final DocumentWriter documentWriter = new DocumentWriter(document);
            encodeEntity(documentWriter, value, encoderContext);
            document = documentWriter.getRoot();
            mappedClass.callLifecycleMethods(PostPersist.class, value, document, getMapper());

            getRegistry().get(Document.class).encode(writer, document, encoderContext);
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

        private <S> void encodeValue(final BsonWriter writer,  final EncoderContext encoderContext, final PropertyModel<S> propertyModel,
                                     final S propertyValue) {
            if (propertyModel.shouldSerialize(propertyValue)) {
                writer.writeName(propertyModel.getReadName());
                if (propertyValue == null) {
                    writer.writeNull();
                } else {
                    try {
                        encoderContext.encodeWithChildContext(propertyModel.getCachedCodec(), writer, propertyValue);
                    } catch (CodecConfigurationException e) {
                        throw new CodecConfigurationException(format("Failed to encode '%s'. Encoding '%s' errored with: %s",
                            getClassModel().getName(), propertyModel.getReadName(), e.getMessage()), e);
                    }
                }
            }
        }

        @Override
        public Class<T> getEncoderClass() {
            return MorphiaCodec.this.getEncoderClass();
        }

        protected <S, V> boolean areEquivalentTypes(final Class<S> t1, final Class<V> t2) {
            return t1.equals(t2)
                   || Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)
                   || Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2);
        }

        /**
         * Creates a specialized codec that honors the current useDiscriminator setting.  For use in case where the field's declared type is
         * a parent type but the actual value is a subtype.
         *
         * @param type the actual type
         * @return potentially specialized Codec
         */
        private Codec<T> specializeCodec(final Class<T> type) {
            Codec<T> codec = getRegistry().get(type);
            if (codec instanceof MorphiaCodec) {
                codec = ((MorphiaCodec<T>) codec).getSpecializedCodec(datastore);
            }
            return codec;
        }
    }
}
