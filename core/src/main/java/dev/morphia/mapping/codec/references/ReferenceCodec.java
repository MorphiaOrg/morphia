package dev.morphia.mapping.codec.references;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.PropertyCodec;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.experimental.ListReference;
import dev.morphia.mapping.experimental.MapReference;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.experimental.SetReference;
import dev.morphia.mapping.experimental.SingleReference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.sofia.Sofia;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class ReferenceCodec extends PropertyCodec<Object> implements PropertyHandler {
    private final Reference annotation;
    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();

    /**
     * Creates a codec
     *
     * @param datastore the datastore to use
     * @param field     the reference field
     * @param typeData  the field type data
     */
    public ReferenceCodec(Datastore datastore, Field field, TypeData typeData) {
        super(datastore, field, typeData);
        annotation = field.getAnnotation(Reference.class);
    }

    /**
     * Encodes a value
     *
     * @param mapper    the mapper to use
     * @param datastore the datastore to use
     * @param value     the value to encode
     * @param model     the mapped class of the field type
     * @return the encoded value
     * @morphia.internal
     */
    public static Object encodeId(Mapper mapper, Datastore datastore, Object value, FieldModel model) {
        Object idValue;
        MongoCollection<?> collection = null;
        if (value instanceof Key) {
            idValue = ((Key) value).getId();
            collection = datastore.getDatabase().getCollection(((Key<?>) value).getCollection(), ((Key<?>) value).getType());
        } else {
            idValue = mapper.getId(value);
            if (idValue == null) {
                return !mapper.isMappable(value.getClass()) ? value : null;
            }
            collection = mapper.getCollection(value.getClass());
        }

        String valueCollectionName = collection.getNamespace().getCollectionName();

        Reference annotation = model.getAnnotation(Reference.class);
        if (annotation != null && !annotation.idOnly()) {
            if (idValue == null) {
                throw new MappingException("The ID value can not be null");
            }
            idValue = new DBRef(valueCollectionName, idValue);
        }
        return idValue;
    }

    /**
     * Encodes a value
     *
     * @param mapper    the mapper to use
     * @param datastore the datastore to use
     * @param value     the value to encode
     * @param model     the mapped class of the field type
     * @return the encoded value
     * @morphia.internal
     */
    public static Object encodeId(Mapper mapper, Datastore datastore, Object value, EntityModel model) {
        Object idValue;
        MongoCollection<?> collection = null;
        if (value instanceof Key) {
            idValue = ((Key) value).getId();
        } else {
            idValue = mapper.getId(value);
            if (idValue == null) {
                return !mapper.isMappable(value.getClass()) ? value : null;
            }
            collection = mapper.getCollection(value.getClass());
        }

        String valueCollectionName = collection != null ? collection.getNamespace().getCollectionName() : null;
        String fieldCollectionName = model.getCollectionName();

        Reference annotation = model.getAnnotation(Reference.class);
        if (annotation != null && !annotation.idOnly()
            || valueCollectionName != null && !valueCollectionName.equals(fieldCollectionName)) {
            if (idValue == null) {
                throw new MappingException("The ID value can not be null");
            }
            idValue = new DBRef(valueCollectionName, idValue);
        }
        return idValue;
    }

    /**
     * Decodes an ID value
     *
     * @param decode         the value to decode
     * @param mapper         the mapper to use
     * @param decoderContext the decoder context
     * @return the decoded value
     */
    public static Object processId(Object decode, Mapper mapper, DecoderContext decoderContext) {
        Object id = decode;
        if (id instanceof Iterable) {
            Iterable iterable = (Iterable) id;
            List ids = new ArrayList();
            for (Object o : iterable) {
                ids.add(processId(o, mapper, decoderContext));
            }
            id = ids;
        } else if (id instanceof Document) {
            Document document = (Document) id;
            if (document.containsKey("$ref")) {
                id = processId(new DBRef(document.getString("$db"), document.getString("$ref"), document.get("$id")),
                    mapper, decoderContext);
            } else if (document.containsKey(mapper.getOptions().getDiscriminatorKey())) {
                try {
                    id = mapper.getCodecRegistry()
                               .get(mapper.getClass(document))
                               .decode(new DocumentReader(document), decoderContext);
                } catch (CodecConfigurationException e) {
                    throw new MappingException(Sofia.cannotFindTypeInDocument(), e);
                }

            }
        } else if (id instanceof DBRef) {
            DBRef ref = (DBRef) id;
            Object refId = ref.getId();
            if (refId instanceof Document) {
                refId = mapper.getCodecRegistry()
                              .get(Object.class)
                              .decode(new DocumentReader((Document) refId), decoderContext);
            }
            id = new DBRef(ref.getDatabaseName(), ref.getCollectionName(), refId);
        }
        return id;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        Object decode = getDatastore().getMapper().getCodecRegistry()
                                      .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                                      .decode(reader, decoderContext);
        decode = processId(decode, getDatastore().getMapper(), decoderContext);
        return fetch(decode);
    }

    @Override
    public Object encode(Object value) {
        try {
            DocumentWriter writer = new DocumentWriter();
            document(writer, () -> {
                writer.writeName("ref");
                encode(writer, value, EncoderContext.builder().build());
            });
            return writer.getDocument().get("ref");
        } catch (ReferenceException e) {
            return value;
        }
    }

    @Override
    public void encode(BsonWriter writer, Object instance, EncoderContext encoderContext) {
        Object idValue = collectIdValues(instance);

        if (idValue != null) {
            final Codec codec = getDatastore().getMapper().getCodecRegistry().get(idValue.getClass());
            codec.encode(writer, idValue, encoderContext);
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

    private Object collectIdValues(Object value) {
        if (value instanceof Collection) {
            List ids = new ArrayList(((Collection) value).size());
            for (Object o : (Collection) value) {
                ids.add(collectIdValues(o));
            }
            return ids;
        } else if (value instanceof Map) {
            final LinkedHashMap ids = new LinkedHashMap();
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> o : map.entrySet()) {
                ids.put(o.getKey().toString(), collectIdValues(o.getValue()));
            }
            return ids;
        } else if (value.getClass().isArray()) {
            List ids = new ArrayList(((Object[]) value).length);
            for (Object o : (Object[]) value) {
                ids.add(collectIdValues(o));
            }
            return ids;
        } else {
            //
            //
            //            clean up before commit
            //
            //
            //
            Object id = encodeId(getDatastore().getMapper(), getDatastore(), value, getEntityModelForField());
            Object id1 = encodeId(getDatastore().getMapper(), getDatastore(), value, getFieldModel());
            return id1;
        }
    }

    private <T> T createProxy(MorphiaReference reference) {
        ReferenceProxy referenceProxy = new ReferenceProxy(reference);
        try {
            Class<?> type = getField().getType();
            String name = (type.getPackageName().startsWith("java") ? type.getSimpleName() : type.getName()) + "$$Proxy";
            return ((Loaded<T>) new ByteBuddy()
                                    .subclass(type)
                                    .implement(MorphiaProxy.class)
                                    .name(name)

                                    .invokable(ElementMatchers.isDeclaredBy(type))
                                    .intercept(InvocationHandlerAdapter.of(referenceProxy))

                                    .method(ElementMatchers.isDeclaredBy(MorphiaProxy.class))
                                    .intercept(InvocationHandlerAdapter.of(referenceProxy))

                                    .make()
                                    .load(Thread.currentThread().getContextClassLoader(), Default.WRAPPER))
                       .getLoaded()
                       .getDeclaredConstructor()
                       .newInstance();
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    private Object fetch(Object value) {
        MorphiaReference reference;
        final Class<?> type = getField().getType();
        if (List.class.isAssignableFrom(type)) {
            reference = readList((List) value);
        } else if (Map.class.isAssignableFrom(type)) {
            reference = readMap((Map<Object, Object>) value);
        } else if (Set.class.isAssignableFrom(type)) {
            reference = readSet((List) value);
        } else if (type.isArray()) {
            reference = readList((List) value);
        } else if (value instanceof Document) {
            reference = readDocument((Document) value);
        } else {
            reference = readSingle(value);
        }
        reference.ignoreMissing(annotation.ignoreMissing());

        return !annotation.lazy() ? reference.get() : createProxy(reference);
    }

    MorphiaReference readDocument(Document value) {
        Mapper mapper = getDatastore().getMapper();
        final Object id = mapper.getCodecRegistry().get(Object.class)
                                .decode(new DocumentReader(value), DecoderContext.builder().build());
        return readSingle(id);
    }

    private List mapToEntitiesIfNecessary(List value) {
        Mapper mapper = getDatastore().getMapper();
        Codec<?> codec = mapper.getCodecRegistry().get(getEntityModelForField().getType());
        return (List) value.stream()
                           .filter(v -> v instanceof Document && ((Document) v).containsKey("_id"))
                           .map(d -> codec.decode(new DocumentReader((Document) d), DecoderContext.builder().build()))
                           .collect(Collectors.toList());
    }

    MorphiaReference readList(List value) {
        List mapped = mapToEntitiesIfNecessary(value);
        return mapped.isEmpty()
               ? new ListReference(getDatastore(), getEntityModelForField(), value)
               : new ListReference(mapped);
    }

    MorphiaReference readSet(List value) {
        List mapped = mapToEntitiesIfNecessary(value);
        return mapped.isEmpty()
               ? new SetReference(getDatastore(), getEntityModelForField(), value)
               : new SetReference(new LinkedHashSet(mapped));
    }

    MorphiaReference readMap(Map<Object, Object> value) {
        final Object ids = new LinkedHashMap<>();
        Class keyType = ((TypeData) getTypeData().getTypeParameters().get(0)).getType();
        for (Entry entry : value.entrySet()) {
            ((Map) ids).put(Conversions.convert(entry.getKey(), keyType), entry.getValue());
        }

        return new MapReference(getDatastore(), (Map<String, Object>) ids, getEntityModelForField());
    }

    MorphiaReference readSingle(Object value) {
        return new SingleReference(getDatastore(), getEntityModelForField(), value);
    }
}

