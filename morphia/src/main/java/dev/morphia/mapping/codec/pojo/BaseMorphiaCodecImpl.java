package dev.morphia.mapping.codec.pojo;

import dev.morphia.mapping.codec.MorphiaCodec;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodec;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeData;
import org.bson.codecs.pojo.TypeParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class BaseMorphiaCodecImpl<T> implements MorphiaCodec<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("PojoCodec");
    private final ClassModel<T> classModel;
    private final CodecRegistry registry;
    private final PropertyCodecRegistry propertyCodecRegistry;
    private final DiscriminatorLookup discriminatorLookup;
    private final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache;
    private final boolean specialized;

    public BaseMorphiaCodecImpl(final ClassModel<T> classModel, final CodecRegistry codecRegistry,
                                final List<PropertyCodecProvider> propertyCodecProviders, final DiscriminatorLookup discriminatorLookup) {
        this.classModel = classModel;
        this.registry = fromRegistries(fromCodecs(this), codecRegistry);
        this.discriminatorLookup = discriminatorLookup;
        this.codecCache = new ConcurrentHashMap<ClassModel<?>, Codec<?>>();
        this.propertyCodecRegistry = new PropertyCodecRegistryImpl(this, registry, propertyCodecProviders);
        this.specialized = shouldSpecialize(classModel);
        specialize();
    }

    public BaseMorphiaCodecImpl(final ClassModel<T> classModel, final CodecRegistry registry, final PropertyCodecRegistry propertyCodecRegistry,
                                final DiscriminatorLookup discriminatorLookup, final ConcurrentMap<ClassModel<?>, Codec<?>> codecCache,
                                final boolean specialized) {
        this.classModel = classModel;
        this.registry = fromRegistries(fromCodecs(this), registry);
        this.discriminatorLookup = discriminatorLookup;
        this.codecCache = codecCache;
        this.propertyCodecRegistry = propertyCodecRegistry;
        this.specialized = specialized;
        specialize();
    }

    protected CodecRegistry getRegistry() {
        return registry;
    }

    protected PropertyCodecRegistry getPropertyCodecRegistry() {
        return propertyCodecRegistry;
    }

    protected DiscriminatorLookup getDiscriminatorLookup() {
        return discriminatorLookup;
    }

    private void specialize() {
        if (specialized) {
            codecCache.put(classModel, this);
            for (PropertyModel<?> propertyModel : classModel.getPropertyModels()) {
                try {
                    addToCache(propertyModel);
                } catch (Exception e) {
                    throw new CodecConfigurationException(format("Could not create a PojoCodec for '%s'."
                                                                 + " Property '%s' errored with: %s", classModel.getName(), propertyModel.getName(), e.getMessage()), e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        if (!specialized) {
            throw new CodecConfigurationException(format("%s contains generic types that have not been specialised.%n"
                            + "Top level classes with generic types are not supported by the PojoCodec.", classModel.getName()));
        }
        if (areEquivalentTypes(value.getClass(), classModel.getType())) {
            writer.writeStartDocument();
            PropertyModel<?> idPropertyModel = classModel.getIdPropertyModel();
            if (idPropertyModel != null) {
                encodeProperty(writer, value, encoderContext, idPropertyModel);
            }

            if (classModel.useDiscriminator()) {
                writer.writeString(classModel.getDiscriminatorKey(), classModel.getDiscriminator());
            }

            for (PropertyModel<?> propertyModel : classModel.getPropertyModels()) {
                if (propertyModel.equals(classModel.getIdPropertyModel())) {
                    continue;
                }
                encodeProperty(writer, value, encoderContext, propertyModel);
            }
            writer.writeEndDocument();
        } else {
            specializeCodec(value.getClass()).encode(writer, value, encoderContext);
        }
    }

    /**
     * Creates a specialized codec that honors the current useDiscriminator setting.  For use in case where the field's declared type is
     * a parent type but the actual value is a subtype.
     *
     * @param type the actual type
     * @return potentially specialized Codec
     */
    @SuppressWarnings("unchecked")
    private Codec specializeCodec(final Class<?> type) {
        Codec<?> codec = registry.get(type);
        if(codec instanceof BaseMorphiaCodecImpl) {
            final BaseMorphiaCodecImpl<?> tCodec = (BaseMorphiaCodecImpl<?>) codec;
            final ClassModel<?> clazzModel = tCodec.getClassModel();
            final ClassModel<?> newModel = new ClassModel(clazzModel.getType(), clazzModel.getPropertyNameToTypeParameterMap(),
                clazzModel.getInstanceCreatorFactory(), true, clazzModel.getDiscriminatorKey(),
                clazzModel.getDiscriminator(), clazzModel.getIdPropertyModelHolder(), clazzModel.getAnnotations(), clazzModel.getFieldModels(),
                clazzModel.getPropertyModels());
            codec = tCodec.getSpecializedCodec(newModel);
        }
        return codec;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        if (decoderContext.hasCheckedDiscriminator()) {
            if (!specialized) {
                throw new CodecConfigurationException(format("%s contains generic types that have not been specialised.%n"
                        + "Top level classes with generic types are not supported by the PojoCodec.", classModel.getName()));
            }
            InstanceCreator<T> instanceCreator = classModel.getInstanceCreator();
            decodeProperties(reader, decoderContext, instanceCreator);
            return instanceCreator.getInstance();
        } else {
            return getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(), registry,
                    discriminatorLookup, this).decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return classModel.getType();
    }

    @Override
    public String toString() {
        return format("PojoCodec<%s>", classModel);
    }

    @Override
    public ClassModel<T> getClassModel() {
        return classModel;
    }

    @SuppressWarnings("unchecked")
    protected  <S> void encodeProperty(final BsonWriter writer, final T instance, final EncoderContext encoderContext,
                                    final PropertyModel<S> propertyModel) {
        if (propertyModel.isReadable()) {
            S propertyValue = propertyModel.getPropertyAccessor().get(instance);
            if (propertyModel.shouldSerialize(propertyValue)) {
                writer.writeName(propertyModel.getReadName());
                if (propertyValue == null) {
                    writer.writeNull();
                } else {
                    propertyModel.getCachedCodec().encode(writer, propertyValue, encoderContext);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void decodeProperties(final BsonReader reader, final DecoderContext decoderContext,
                                    final InstanceCreator<T> instanceCreator) {
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(name)) {
                reader.readString();
            } else {
                decodePropertyModel(reader, decoderContext, instanceCreator, name, getPropertyModelByWriteName(classModel, name));
            }
        }
        reader.readEndDocument();
    }

    @SuppressWarnings("unchecked")
    protected <S> void decodePropertyModel(final BsonReader reader, final DecoderContext decoderContext,
                                         final InstanceCreator<T> instanceCreator, final String name,
                                         final PropertyModel<S> propertyModel) {
        if (propertyModel != null) {
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
                throw new CodecConfigurationException(format("Failed to decode '%s'. Decoding '%s' errored with: %s",
                        classModel.getName(), name, e.getMessage()), e);
            } catch (CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Failed to decode '%s'. Decoding '%s' errored with: %s",
                        classModel.getName(), name, e.getMessage()), e);
            }
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(format("Found property not present in the ClassModel: %s", name));
            }
            reader.skipValue();
        }
    }

    private <S> void addToCache(final PropertyModel<S> propertyModel) {
        Codec<S> codec = propertyModel.getCodec() != null ? propertyModel.getCodec()
                : specializePojoCodec(propertyModel, propertyCodecRegistry.get(propertyModel.getTypeData()));
        propertyModel.cachedCodec(codec);
    }

    private <S, V> boolean areEquivalentTypes(final Class<S> t1, final Class<V> t2) {
        if (t1.equals(t2)) {
            return true;
        } else if (Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)) {
            return true;
        } else if (Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected <S> Codec<S> specializePojoCodec(final PropertyModel<S> propertyModel, final Codec<S> defaultCodec) {
        Codec<S> codec = defaultCodec;
        if (codec instanceof MorphiaCodec) {
            MorphiaCodec<S> morphiaCodec = (MorphiaCodec<S>) codec;
            ClassModel<S> specialized = getSpecializedClassModel(morphiaCodec.getClassModel(), propertyModel);
            if (codecCache.containsKey(specialized)) {
                codec = (Codec<S>) codecCache.get(specialized);
            } else {
                codec = getSpecializedCodec(specialized);
            }
        }
        return codec;
    }

    protected <S> MorphiaCodec<S> getSpecializedCodec(final ClassModel<S> specialized) {
        return new LazyPojoCodec<S>(specialized, registry, propertyCodecRegistry, discriminatorLookup, codecCache);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S, V> ClassModel<S> getSpecializedClassModel(final ClassModel<S> clazzModel, final PropertyModel<V> propertyModel) {
        boolean useDiscriminator = propertyModel.useDiscriminator() == null ? clazzModel.useDiscriminator()
                : propertyModel.useDiscriminator();
        boolean validDiscriminator = clazzModel.getDiscriminatorKey() != null && clazzModel.getDiscriminator() != null;
        boolean changeTheDiscriminator = (useDiscriminator != clazzModel.useDiscriminator()) && validDiscriminator;

        if (propertyModel.getTypeData().getTypeParameters().isEmpty() && !changeTheDiscriminator){
            return clazzModel;
        }

        List<PropertyModel<?>> concretePropertyModels = new ArrayList<PropertyModel<?>>(clazzModel.getPropertyModels());
        PropertyModel<?> concreteIdProperty = clazzModel.getIdPropertyModel();

        List<TypeData<?>> propertyTypeParameters = propertyModel.getTypeData().getTypeParameters();
        for (int i = 0; i < concretePropertyModels.size(); i++) {
            PropertyModel<?> model = concretePropertyModels.get(i);
            String propertyName = model.getName();
            TypeParameterMap typeParameterMap = clazzModel.getPropertyNameToTypeParameterMap().get(propertyName);
            if (typeParameterMap.hasTypeParameters()) {
                PropertyModel<?> concretePropertyModel = getSpecializedPropertyModel(model, typeParameterMap, propertyTypeParameters);
                concretePropertyModels.set(i, concretePropertyModel);
                if (concreteIdProperty != null && concreteIdProperty.getName().equals(propertyName)) {
                    concreteIdProperty = concretePropertyModel;
                }
            }
        }

        boolean discriminatorEnabled = changeTheDiscriminator ? propertyModel.useDiscriminator() : clazzModel.useDiscriminator();
        return new ClassModel<S>(clazzModel.getType(), clazzModel.getPropertyNameToTypeParameterMap(),
            clazzModel.getInstanceCreatorFactory(), discriminatorEnabled, clazzModel.getDiscriminatorKey(),
            clazzModel.getDiscriminator(), concreteIdProperty, clazzModel.getAnnotations(), clazzModel.getFieldModels(),
            concretePropertyModels);
    }

    @SuppressWarnings("unchecked")
    private <V> PropertyModel<V> getSpecializedPropertyModel(final PropertyModel<V> propertyModel, final TypeParameterMap typeParameterMap,
                                                             final List<TypeData<?>> propertyTypeParameters) {
        TypeData<V> specializedPropertyType;
        Map<Integer, Integer> propertyToClassParamIndexMap = typeParameterMap.getPropertyToClassParamIndexMap();
        Integer classTypeParamRepresentsWholeProperty = propertyToClassParamIndexMap.get(-1);
        if (classTypeParamRepresentsWholeProperty != null) {
            specializedPropertyType = (TypeData<V>) propertyTypeParameters.get(classTypeParamRepresentsWholeProperty);
        } else {
            TypeData.Builder<V> builder = TypeData.builder(propertyModel.getTypeData().getType());
            List<TypeData<?>> typeParameters = new ArrayList<TypeData<?>>(propertyModel.getTypeData().getTypeParameters());
            for (int i = 0; i < typeParameters.size(); i++) {
                for (Map.Entry<Integer, Integer> mapping : propertyToClassParamIndexMap.entrySet()) {
                    if (mapping.getKey().equals(i)) {
                        typeParameters.set(i, propertyTypeParameters.get(mapping.getValue()));
                    }
                }
            }
            builder.addTypeParameters(typeParameters);
            specializedPropertyType = builder.build();
        }
        if (propertyModel.getTypeData().equals(specializedPropertyType)) {
            return propertyModel;
        }

        return new PropertyModel<V>(propertyModel.getName(), propertyModel.getReadName(), propertyModel.getWriteName(),
                specializedPropertyType, null, propertyModel.getPropertySerialization(), propertyModel.useDiscriminator(),
                propertyModel.getPropertyAccessor());
    }

    @SuppressWarnings("unchecked")
    private Codec<T> getCodecFromDocument(final BsonReader reader, final boolean useDiscriminator, final String discriminatorKey,
                                          final CodecRegistry registry, final DiscriminatorLookup discriminatorLookup,
                                          final Codec<T> defaultCodec) {
        Codec<T> codec = defaultCodec;
        if (useDiscriminator) {
            BsonReaderMark mark = reader.getMark();
            reader.readStartDocument();
            boolean discriminatorKeyFound = false;
            while (!discriminatorKeyFound && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (discriminatorKey.equals(name)) {
                    discriminatorKeyFound = true;
                    try {
                        codec = (Codec<T>) registry.get(discriminatorLookup.lookup(reader.readString()));
                    } catch (Exception e) {
                        throw new CodecConfigurationException(format("Failed to decode '%s'. Decoding errored with: %s",
                                classModel.getName(), e.getMessage()), e);
                    }
                } else {
                    reader.skipValue();
                }
            }
            mark.reset();
        }
        return codec;
    }

    private PropertyModel<?> getPropertyModelByWriteName(final ClassModel<T> classModel, final String readName) {
        for (PropertyModel<?> propertyModel : classModel.getPropertyModels()) {
            if (propertyModel.isWritable() && propertyModel.getWriteName().equals(readName)) {
                return propertyModel;
            }
        }
        return null;
    }

    private static <T> boolean shouldSpecialize(final ClassModel<T> classModel) {
        if (!classModel.hasTypeParameters()) {
            return true;
        }

        for (Map.Entry<String, TypeParameterMap> entry : classModel.getPropertyNameToTypeParameterMap().entrySet()) {
            TypeParameterMap typeParameterMap = entry.getValue();
            PropertyModel<?> propertyModel = classModel.getPropertyModel(entry.getKey());
            if (typeParameterMap.hasTypeParameters() && (propertyModel == null || propertyModel.getCodec() == null)) {
                return false;
            }
        }
        return true;
    }
}