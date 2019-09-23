package dev.morphia.mapping.codec.references;

import com.mongodb.DBRef;
import com.mongodb.DocumentToDBRefTransformer;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedField;
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
    private MappedField idField;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private DocumentToDBRefTransformer transformer = new DocumentToDBRefTransformer();

    public ReferenceCodec(final Datastore datastore, final Field field, final String name, final TypeData typeData) {
        super(datastore, field, name, typeData);
        annotation = field.getAnnotation(Reference.class);
    }

    private MappedField getIdField() {
        if (idField == null) {
            idField = getFieldMappedClass().getIdField();
        }
        return idField;
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext decoderContext) {

        Object decode = getDatastore().getMapper().getCodecRegistry()
                                      .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                                      .decode(reader, decoderContext);
        if (decode instanceof Document) {
            Document document = (Document) decode;
            if (document.containsKey("$ref")) {
                decode = document.get("$id");
                if (decode instanceof Document) {
                    DocumentReader documentReader = new DocumentReader((Document) decode);
                    decode = getDatastore().getMapper().getCodecRegistry()
                                           .get(Object.class)
                                           .decode(documentReader, decoderContext);
                }
            }
        }
        return fetch(decode);
    }

    public Object fetch(final Object value) {
        MorphiaReference reference;
        Object ids;
        if (value instanceof List) {
            ids = new ArrayList<>();
            for (Object o : (List) value) {
                ((List) ids).add(extractId(o));
            }
            reference = new ListReference(getDatastore(), getFieldMappedClass(), (List) ids);
        } else if (value instanceof Map) {
            ids = new LinkedHashMap<>();
            Set<Entry<Object, Object>> set = ((Map<Object, Object>) value).entrySet();
            Class keyType = ((TypeData) getTypeData().getTypeParameters().get(0)).getType();
            for (final Entry entry : set) {
                ((Map) ids).put(Conversions.convert(entry.getKey(), keyType), extractId(entry.getValue()));
            }

            reference = new MapReference(getDatastore(), getFieldMappedClass(), (Map<String, Object>) ids);
        } else if (value instanceof Set) {
            ids = new ArrayList<>();
            for (Object o : (Set) value) {
                ((List) ids).add(extractId(o));
            }
            reference = new SetReference(getDatastore(), getFieldMappedClass(), (List) ids);
        } else {
            ids = extractId(value);
            reference = new SingleReference(getDatastore(), getFieldMappedClass(), ids);
        }

        return !annotation.lazy() ? reference.get()
                                  : createProxy(new ReferenceProxy(ids, reference
                                                                            .ignoreMissing(annotation.ignoreMissing())));
    }

    private <T> T createProxy(final ReferenceProxy referenceProxy) {
        try {
            Class<?> type = getField().getType();
            String name = (type.getPackageName().startsWith("java") ? type.getSimpleName() : type.getName()) + "$$Proxy";
            return ((Loaded<T>) new ByteBuddy()
                                    .subclass(type)
                                    .name(name)
                                    .invokable(ElementMatchers.isDeclaredBy(type))
                                    .intercept(InvocationHandlerAdapter.of(referenceProxy))
                                    .make()
                                    .load(type.getClassLoader(), Default.WRAPPER))
                       .getLoaded()
                       .getDeclaredConstructor()
                       .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public Class<Object> getEncoderClass() {
        return Object.class;
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
            return encodeId(value);
        }

    }

    private Object encodeId(final Object value) {
        Object idValue;
        if (value instanceof Key) {
            idValue = ((Key) value).getId();
        } else {
            idValue = getIdField().getFieldValue(value);
        }
        if (!getField().getAnnotation(Reference.class).idOnly()) {
            if (idValue == null) {
                throw new MappingException("The ID value can not be null");
            }
            idValue = new DBRef(getFieldMappedClass().getCollectionName(), idValue);
        }
        return idValue;
    }

    private Object extractId(final Object o) {
        if (annotation.idOnly()) {
            return o;
        }
        if (o instanceof DBRef) {
            Object id = ((DBRef) transformer.transform(o)).getId();
            if (id instanceof Document) {
                id = getDatastore().getMapper().fromDocument(null, (Document) id);
            }
            return id;
        } else {
            return o;
        }
    }
}

