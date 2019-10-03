package dev.morphia.mapping.codec.references;

import com.mongodb.DBRef;
import com.mongodb.DocumentToDBRefTransformer;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.PropertyCodec;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.experimental.ListReference;
import dev.morphia.mapping.experimental.MapReference;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.experimental.SetReference;
import dev.morphia.mapping.experimental.SingleReference;
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
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ReferenceCodec extends PropertyCodec<Object> {
    private final Reference annotation;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private DocumentToDBRefTransformer transformer = new DocumentToDBRefTransformer();

    public ReferenceCodec(final Datastore datastore, final Field field, final String name, final TypeData typeData) {
        super(datastore, field, name, typeData);
        annotation = field.getAnnotation(Reference.class);
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext decoderContext) {
        Object decode = getDatastore().getMapper().getCodecRegistry()
                                      .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                                      .decode(reader, decoderContext);
        decode = processId(decode, getDatastore().getMapper(), decoderContext);
        return fetch(decode);
    }

    public static Object processId(Object decode, final Mapper mapper, final DecoderContext decoderContext) {
        if (decode instanceof Document) {
            Document document = (Document) decode;
            if (document.containsKey("$ref")) {
                Object id = document.get("$id");
                if (id instanceof Document) {
                    DocumentReader documentReader = new DocumentReader((Document) id);
                    id = mapper.getCodecRegistry()
                               .get(Object.class)
                               .decode(documentReader, decoderContext);
                }
                decode = new DBRef((String) document.get("$ref"), id);
            }
        }
        return decode;
    }

    public Object fetch(final Object value) {
        MorphiaReference reference;
        if (value instanceof List) {
            reference = readList((List) value);
        } else if (value instanceof Document) {
            reference = readDocument((Document) value);
        } else if (value instanceof Map) {
            reference = readMap((Map<Object, Object>) value);
        } else if (value instanceof Set) {
            reference = readSet((Set) value);
        } else {
            reference = readSingle(value);
        }
        reference.ignoreMissing(annotation.ignoreMissing());

        return !annotation.lazy() ? reference.get() : createProxy(reference);
    }

    MorphiaReference readDocument(final Document value) {
        final MorphiaReference reference;
        Mapper mapper = getDatastore().getMapper();
        Document document = value;
        final Object id = mapper.getCodecRegistry().get(Object.class)
                                .decode(new DocumentReader(document), DecoderContext.builder().build());
        reference = readSingle(id);
        return reference;
    }

    MorphiaReference readSingle(final Object value) {
        return new SingleReference(getDatastore(), getFieldMappedClass(), value);
    }

    MorphiaReference readSet(final Set value) {
        return new SetReference(getDatastore(), getFieldMappedClass(), new ArrayList(value));
    }

    MorphiaReference readMap(final Map<Object, Object> value) {
        final Object ids = new LinkedHashMap<>();
        Class keyType = ((TypeData) getTypeData().getTypeParameters().get(0)).getType();
        for (final Entry entry : value.entrySet()) {
            ((Map) ids).put(Conversions.convert(entry.getKey(), keyType), entry.getValue());
        }

        return new MapReference(getDatastore(), getFieldMappedClass(), (Map<String, Object>) ids);
    }

    MorphiaReference readList(final List value) {
        return new ListReference(getDatastore(), getFieldMappedClass(), value);
    }

    private <T> T createProxy(final MorphiaReference reference) {
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
                                    .load(type.getClassLoader(), Default.WRAPPER))
                       .getLoaded()
                       .getDeclaredConstructor()
                       .newInstance();
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public Class getEncoderClass() {
        return getField().getType();
    }

    @Override
    public void encode(final BsonWriter writer, final Object instance, final EncoderContext encoderContext) {
        Object idValue = collectIdValues(instance);

        final Codec codec = getDatastore().getMapper().getCodecRegistry().get(idValue.getClass());
        codec.encode(writer, idValue, encoderContext);
    }

    private Object collectIdValues(final Object value) {
        if (value instanceof Collection) {
            List ids = new ArrayList(((Collection) value).size());
            for (Object o : (Collection) value) {
                ids.add(collectIdValues(o));
            }
            return ids;
        } else if (value instanceof Map) {
            final LinkedHashMap ids = new LinkedHashMap();
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (final Map.Entry<Object, Object> o : map.entrySet()) {
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
            return encodeId(getDatastore().getMapper(), getFieldMappedClass(), value);
        }
    }

    /**
     * @morphia.internal
     */
    public static Object encodeId(final Mapper mapper, final MappedClass fieldMappedClass, final Object value) {
        Object idValue;
        if (value instanceof Key) {
            idValue = ((Key) value).getId();
        } else {
            idValue = mapper.getId(value);
        }
        MongoCollection<?> collection = null;
        try {
            collection = mapper.getCollection(value.getClass());
        } catch (NullPointerException e) {
            System.out.println(value);

        }
        String valueCollectionName = collection != null ? collection.getNamespace().getCollectionName() : null;
        String fieldCollectionName = fieldMappedClass.getCollectionName();

        Reference annotation = fieldMappedClass.getAnnotation(Reference.class);
        if (annotation != null && !annotation.idOnly()
            || valueCollectionName != null && !valueCollectionName.equals(fieldCollectionName)) {
            if (idValue == null) {
                throw new MappingException("The ID value can not be null");
            }
            idValue = new DBRef(valueCollectionName, idValue);
        }
        return idValue;
    }
}

