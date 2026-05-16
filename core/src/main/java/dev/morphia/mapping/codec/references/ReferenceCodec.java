package dev.morphia.mapping.codec.references;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.QueryException;
import dev.morphia.sofia.Sofia;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.TypeCache.Sort;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.in;
import static java.lang.String.format;

/**
 * @hidden
 * @morphia.internal
 */
@SuppressWarnings({ "unchecked", "removal" })
@MorphiaInternal
public class ReferenceCodec extends BaseReferenceCodec<Object> implements PropertyHandler {
    private static final String FIELD_INVOCATION_HANDLER = "handler";

    private final Reference annotation;
    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private final Mapper mapper;
    private final ClassLoader classLoader;
    private final TypeCache<TypeCache.SimpleKey> typeCache = new TypeCache.WithInlineExpunction<>(Sort.SOFT);
    private MorphiaDatastore datastore;

    /**
     * Creates a codec
     *
     * @param datastore
     * @param propertyModel the reference property
     */
    public ReferenceCodec(MorphiaDatastore datastore, PropertyModel propertyModel) {
        super(datastore, propertyModel);
        this.datastore = datastore;
        this.classLoader = datastore.getClassLoader();
        this.mapper = datastore.getMapper();
        annotation = getReferenceAnnotation(propertyModel);
    }

    /**
     * Encodes a value
     *
     * @param mapper
     * @param value  the value to encode
     * @param model  the mapped class of the field type
     * @return the encoded value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public static Object encodeId(Mapper mapper, Object value, EntityModel model) {
        Object idValue;
        Class<?> type;
        idValue = mapper.getId(value);
        if (idValue == null) {
            return !mapper.isMappable(value.getClass()) ? value : null;
        }
        type = value.getClass();

        String valueCollectionName = mapper.getEntityModel(type).collectionName();
        String fieldCollectionName = model.collectionName();

        Reference annotation = model.getAnnotation(Reference.class);
        if (annotation != null && !annotation.idOnly()
                || valueCollectionName != null && !valueCollectionName.equals(fieldCollectionName)) {
            idValue = new DBRef(valueCollectionName, idValue);
        }
        return idValue;
    }

    /**
     * Decodes an ID value
     *
     * @param datastore      the Datastore to use
     * @param decode         the value to decode
     * @param decoderContext the decoder context
     * @return the decoded value
     */
    @NonNull
    public static Object processId(MorphiaDatastore datastore, Object decode, DecoderContext decoderContext) {
        Object id = decode;
        if (id instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) id;
            List<Object> ids = new ArrayList<>();
            for (Object o : iterable) {
                ids.add(processId(datastore, o, decoderContext));
            }
            id = ids;
        } else if (id instanceof Document) {
            Document document = (Document) id;
            if (document.containsKey("$ref")) {
                id = processId(datastore, new DBRef(document.getString("$db"), document.getString("$ref"), document.get("$id")),
                        decoderContext);
            } else if (document.containsKey(datastore.getMapper().getConfig().discriminatorKey())) {
                try {
                    id = datastore.getCodecRegistry()
                            .get(datastore.getMapper().getClass(document))
                            .decode(new DocumentReader(document, datastore.getMapper().getConversions()), decoderContext);
                } catch (CodecConfigurationException e) {
                    throw new MappingException(Sofia.cannotFindTypeInDocument(), e);
                }

            }
        } else if (id instanceof DBRef) {
            DBRef ref = (DBRef) id;
            Object refId = ref.getId();
            if (refId instanceof Document) {
                refId = datastore.getCodecRegistry()
                        .get(Object.class)
                        .decode(new DocumentReader((Document) refId, datastore.getMapper().getConversions()), decoderContext);
            }
            id = new DBRef(ref.getDatabaseName(), ref.getCollectionName(), refId);
        }
        return id;
    }

    @Nullable
    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        Object decode = getDatastore().getCodecRegistry()
                .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                .decode(reader, decoderContext);
        decode = processId(getDatastore(), decode, decoderContext);
        return fetch(decode);
    }

    private static TypeCache.SimpleKey getCacheKey(Class<?> type) {
        return new TypeCache.SimpleKey(type, Arrays.asList(type.getInterfaces()));
    }

    @Override
    @Nullable
    public Object encode(Object value) {
        try {
            DocumentWriter writer = new DocumentWriter(mapper.getConfig());
            document(writer, () -> {
                writer.writeName("ref");
                encode(writer, value, EncoderContext.builder().build());
            });
            return writer.getDocument().get("ref");
        } catch (ReferenceException e) {
            Reference refAnn = getPropertyModel().getAnnotation(Reference.class);
            if (refAnn != null && refAnn.ignoreMissing()) {
                return null;
            } else {
                throw e;
            }

        }
    }

    @Override
    public void encode(BsonWriter writer, @Nullable Object instance, EncoderContext encoderContext) {
        Object idValue = collectIdValues(instance);

        if (idValue != null) {
            final Codec codec = getDatastore().getCodecRegistry().get(idValue.getClass());
            codec.encode(writer, idValue, encoderContext);
        } else if (getReferenceAnnotation(getPropertyModel()).ignoreMissing() || instance == null) {
            writer.writeNull();
        } else {
            throw new ReferenceException(Sofia.noIdForReference());
        }
    }

    @Override
    public Class getEncoderClass() {
        TypeData type = getTypeData();
        List typeParameters = type.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            type = (TypeData) typeParameters.get(typeParameters.size() - 1);
        }
        return type.getType();
    }

    @Nullable
    private Object collectIdValues(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                    .map(o -> collectIdValues(o))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else if (value instanceof Map) {
            final Map<Object, Object> ids = new LinkedHashMap<>();
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> o : map.entrySet()) {
                ids.put(o.getKey(), collectIdValues(o.getValue()));
            }
            return ids;
        } else if (value.getClass().isArray()) {
            return Arrays.stream((Object[]) value)
                    .map(o -> collectIdValues(o))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            return encodeId(value);
        }
    }

    private <T> T createProxy(Supplier<Object> loader, List<Object> ids, Class<?> referenceType) {
        ReferenceProxy referenceProxy = new ReferenceProxy(loader, ids, referenceType);
        PropertyModel propertyModel = getPropertyModel();
        try {
            Class<?> type = propertyModel.getType();
            Class<T> proxyClass = (Class<T>) typeCache.findOrInsert(classLoader, getCacheKey(type), this::makeProxy, typeCache);
            final T proxy = proxyClass.getDeclaredConstructor().newInstance();
            final Field field = proxyClass.getDeclaredField(FIELD_INVOCATION_HANDLER);
            field.setAccessible(true);
            field.set(proxy, referenceProxy);
            return proxy;
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    /**
     * Encodes a value
     *
     * @param value the value to encode
     * @return the encoded value
     */
    @Nullable
    private Object encodeId(Object value) {
        Object idValue;
        final String valueCollectionName;
        idValue = mapper.getId(value);
        if (idValue == null && !annotation.ignoreMissing()) {
            if (!mapper.isMappable(value.getClass())) {
                return value;
            }
            throw new QueryException("No ID value found on referenced entity.  Save referenced entities before defining references to"
                    + " them.");
        }
        valueCollectionName = mapper.getEntityModel(value.getClass()).collectionName();

        if (!annotation.idOnly()) {
            idValue = new DBRef(valueCollectionName, idValue);
        }
        return idValue;
    }

    private <T> Class<T> makeProxy() {
        PropertyModel propertyModel = getPropertyModel();
        Class<?> type = propertyModel.getType();
        Builder<?> builder = new ByteBuddy()
                .subclass(type)
                .implement(MorphiaProxy.class)
                .name(format("%s$%s$$ReferenceProxy", propertyModel.getEntityModel().getName(), propertyModel.getName()));

        Junction<ByteCodeElement> matcher = ElementMatchers.isDeclaredBy(type);
        if (!type.isInterface()) {
            type = type.getSuperclass();
            while (type != null && !type.equals(Object.class)) {
                matcher = matcher.or(ElementMatchers.isDeclaredBy(type));
                type = type.getSuperclass();
            }
        }

        return (Class<T>) builder
                .invokable(matcher.or(ElementMatchers.isDeclaredBy(MorphiaProxy.class)))
                .intercept(InvocationHandlerAdapter.toField(FIELD_INVOCATION_HANDLER))
                .defineField(FIELD_INVOCATION_HANDLER, InvocationHandler.class, Visibility.PRIVATE)
                .make()
                .load(classLoader, Default.WRAPPER)
                .getLoaded();
    }

    @Nullable
    private Object fetch(Object value) {
        boolean lazy = annotation.lazy();
        boolean ignoreMissing = annotation.ignoreMissing();
        EntityModel entityModel = getEntityModelForField();
        final Class<?> type = getPropertyModel().getType();

        if (List.class.isAssignableFrom(type) || type.isArray()) {
            List<?> rawIds = (List<?>) value;
            List<?> preDecoded = decodeEmbeddedEntities(rawIds);
            if (!preDecoded.isEmpty()) {
                return preDecoded;
            }
            List<Object> ids = stripDbRefs(rawIds);
            Supplier<Object> loader = () -> fetchCollection(rawIds, entityModel, ignoreMissing);
            return lazy ? createProxy(loader, ids, entityModel.getType()) : loader.get();

        } else if (Set.class.isAssignableFrom(type)) {
            List<?> rawIds = (List<?>) value;
            List<?> preDecoded = decodeEmbeddedEntities(rawIds);
            if (!preDecoded.isEmpty()) {
                return new LinkedHashSet<>(preDecoded);
            }
            List<Object> ids = stripDbRefs(rawIds);
            Supplier<Object> loader = () -> new LinkedHashSet<>(fetchCollection(rawIds, entityModel, ignoreMissing));
            return lazy ? createProxy(loader, ids, entityModel.getType()) : loader.get();

        } else if (Map.class.isAssignableFrom(type)) {
            Map<Object, Object> rawMap = (Map<Object, Object>) value;
            Class<?> keyType = getTypeData().getTypeParameters().get(0).getType();
            Map<Object, Object> ids = new LinkedHashMap<>();
            for (Entry<Object, Object> entry : rawMap.entrySet()) {
                ids.put(mapper.getConversions().convert(entry.getKey(), keyType), entry.getValue());
            }
            List<Object> idList = stripDbRefs(new ArrayList<>(ids.values()));
            Supplier<Object> loader = () -> fetchMap(ids, entityModel);
            return lazy ? createProxy(loader, idList, entityModel.getType()) : loader.get();

        } else {
            Object id = value instanceof Document ? decodeDocument((Document) value) : value;
            // If processId already decoded the value into an entity instance, return it directly
            if (entityModel.getType().isInstance(id)) {
                return id;
            }
            List<Object> ids = List.of(stripDbRef(id));
            Supplier<Object> loader = () -> fetchSingle(id, entityModel, ignoreMissing);
            return lazy ? createProxy(loader, ids, entityModel.getType()) : loader.get();
        }
    }

    private Object fetchSingle(Object id, EntityModel entityModel, boolean ignoreMissing) {
        var query = id instanceof DBRef
                ? datastore.find(mapper.getClassFromCollection(((DBRef) id).getCollectionName()))
                : datastore.find(entityModel.getType());
        Object result = query.filter(eq("_id", stripDbRef(id))).iterator().tryNext();
        if (result == null && !ignoreMissing) {
            throw new ReferenceException(Sofia.missingReferencedEntity(entityModel.getType().getSimpleName()));
        }
        return result;
    }

    private List<Object> fetchCollection(List<?> ids, EntityModel entityModel, boolean ignoreMissing) {
        Map<String, List<Object>> byCollection = new HashMap<>();
        for (Object id : ids) {
            if (id instanceof DBRef) {
                byCollection.computeIfAbsent(((DBRef) id).getCollectionName(), k -> new ArrayList<>()).add(((DBRef) id).getId());
            } else {
                // nested List items are stored as-is; extractFlatIds flattens them for the query
                byCollection.computeIfAbsent(entityModel.collectionName(), k -> new ArrayList<>()).add(id);
            }
        }

        Map<Object, Object> idMap = new HashMap<>();
        for (Entry<String, List<Object>> entry : byCollection.entrySet()) {
            idMap.putAll(queryCollection(entry.getKey(), extractFlatIds(entry.getValue()), entityModel, ignoreMissing));
        }

        return mapIdsToValues(ids, idMap).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Object, Object> queryCollection(String collection, List<Object> collectionIds, EntityModel entityModel,
            boolean ignoreMissing) {
        Map<Object, Object> idMap = new HashMap<>();
        try (MongoCursor<?> cursor = datastore.find(collection).disableValidation().filter(in("_id", collectionIds)).iterator()) {
            while (cursor.hasNext()) {
                Object entity = cursor.next();
                idMap.put(mapper.getId(entity), entity);
            }
            if (!ignoreMissing && idMap.size() != new HashSet<>(collectionIds).size()) {
                throw new ReferenceException(Sofia.missingReferencedEntities(entityModel.getType().getSimpleName()));
            }
        }
        return idMap;
    }

    private List<Object> mapIdsToValues(List<?> ids, Map<Object, Object> idMap) {
        List<Object> values = new ArrayList<>(Arrays.asList(new Object[ids.size()]));
        for (int i = 0; i < ids.size(); i++) {
            Object id = ids.get(i);
            Object resolved = id instanceof List
                    ? mapIdsToValues((List<?>) id, idMap)
                    : idMap.get(id instanceof DBRef ? ((DBRef) id).getId() : id);
            if (resolved != null) {
                values.set(i, resolved);
            }
        }
        return values;
    }

    private Map<Object, Object> fetchMap(Map<Object, Object> ids, EntityModel entityModel) {
        Map<Object, Object> values = new LinkedHashMap<>();
        for (Entry<Object, Object> entry : ids.entrySet()) {
            DBRef dbRef = entry.getValue() instanceof DBRef
                    ? (DBRef) entry.getValue()
                    : new DBRef(entityModel.collectionName(), entry.getValue());
            try (MongoCursor<Object> cursor = (MongoCursor<Object>) datastore.find(dbRef.getCollectionName())
                    .filter(eq("_id", dbRef.getId())).iterator()) {
                values.put(entry.getKey(), cursor.next());
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractFlatIds(List<Object> ids) {
        List<Object> flat = new ArrayList<>();
        for (Object id : ids) {
            if (id instanceof List) {
                flat.addAll(extractFlatIds((List<Object>) id));
            } else {
                flat.add(id);
            }
        }
        return flat;
    }

    private List<?> decodeEmbeddedEntities(List<?> value) {
        Codec<?> codec = getDatastore().getCodecRegistry().get(getEntityModelForField().getType());
        return value.stream()
                .filter(v -> v instanceof Document && ((Document) v).containsKey("_id"))
                .map(d -> codec.decode(new DocumentReader((Document) d, mapper.getConversions()), DecoderContext.builder().build()))
                .collect(Collectors.toList());
    }

    private Object decodeDocument(Document value) {
        return getDatastore().getCodecRegistry().get(Object.class)
                .decode(new DocumentReader(value, mapper.getConversions()), DecoderContext.builder().build());
    }

    private static Object stripDbRef(Object id) {
        return id instanceof DBRef ? ((DBRef) id).getId() : id;
    }

    private static List<Object> stripDbRefs(List<?> ids) {
        return ids.stream().map(ReferenceCodec::stripDbRef).collect(Collectors.toList());
    }
}
