package dev.morphia.mapping.codec;

import com.mongodb.DBRef;
import com.mongodb.DocumentToDBRefTransformer;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.MappingException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ReferenceCodec<T> extends PropertyCodec<T> {

    private final Reference annotation;
    private final PropertyModel propertyModel;
    private MappedField idField;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private DocumentToDBRefTransformer transformer = new DocumentToDBRefTransformer();

    public ReferenceCodec(final Datastore datastore, final Field field, final String name, final TypeData typeData,
                          final PropertyModel propertyModel) {
        super(datastore, field, name, typeData);
        annotation = field.getAnnotation(Reference.class);
        this.propertyModel = propertyModel;
    }

    private MappedField getIdField() {
        if(idField == null) {
            idField = getFieldMappedClass().getIdField();
        }
        return idField;
    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {

        Object decode = getDatastore().getMapper().getCodecRegistry()
                                     .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                                     .decode(reader, decoderContext);
        if(decode instanceof Document) {
            Document document = (Document) decode;
            if(document.containsKey("$ref")) {
                decode = document.get("$id");
                if(decode instanceof Document) {
                    decode = getDatastore().getMapper().getCodecRegistry()
                                  .get(Object.class)
                                  .decode(reader, decoderContext);
                }
            }
        }
        return (T) decode;
    }

/*
    @Override
    public <S, E> void set(final E instance, final PropertyModel<S> propertyModel, final S value, final Datastore datastore,
                           final Map<Object, Object> entityCache) {
        S fetched = value;
        if(value instanceof List) {
            fetched = loadList((List) value, datastore, entityCache);
        } else if(value instanceof Map) {
            fetched = loadMap((Map) value, datastore, entityCache);
        } else {
            final Object id = extractId(fetched);
            fetched = (S) entityCache.computeIfAbsent(id,
                (k) -> datastore.find((Class<S>) getFieldMappedClass().getType())
                                .filter("_id", id)
                                .first());
            if (fetched == null && !annotation.ignoreMissing()) {
                throw new MappingException("Referenced entity not found");
            }
        }
        propertyModel.getPropertyAccessor().set(instance, fetched);
    }
*/

    <S> S loadList(final List value, final Datastore datastore, final Map<Object, Object> entityCache) {
        List<Object> ids = new ArrayList<>();
        for (Object o : value) {
            ids.add(extractId(o));
        }
        datastore.find((Class<S>) getFieldMappedClass().getType())
                 .filter("_id in", ids)
                 .execute()
                 .forEachRemaining(entity -> {
                     entityCache.putIfAbsent(getIdField().getFieldValue(entity), entity);
                 });

        List list = ids.stream()
                       .map(entityCache::get)
                       .filter(Objects::nonNull)
                       .collect(Collectors.toList());

        if(list.size() < ids.size() && !annotation.ignoreMissing()) {
            throw new MappingException("Referenced entities not found");
        }
        return (S) list;
    }

    <S> S loadMap(final Map value, final Datastore datastore, final Map<Object, Object> entityCache) {
        List<Object> ids = new ArrayList<>();
        for (Object o : value.values()) {
            ids.add(extractId(o));
        }

        datastore.find((Class<S>) getFieldMappedClass().getType())
                 .filter("_id in", ids)
                 .execute()
                 .forEachRemaining(entity -> {
                     entityCache.putIfAbsent(getIdField().getFieldValue(entity), entity);
                 });

        Map map = new LinkedHashMap();
        for (Object id : ids) {
            final Object e = entityCache.get(id);
            if(e != null) {
                map.putIfAbsent(getIdField().getFieldValue(e), e);
            }
        }
        if(map.size() < ids.size() && !annotation.ignoreMissing()) {
            throw new MappingException("Referenced entities not found");
        }
        return (S) map;
    }

    @Override
    public Class<T> getEncoderClass() {
        return (Class<T>) Object.class;
    }

    @Override
    public void encode(final BsonWriter writer, final T instance, final EncoderContext encoderContext) {
        writer.writeName(propertyModel.getReadName());
        Object idValue = collectIdValues(instance);

        final Codec codec = getDatastore().getMapper().getCodecRegistry().get(idValue.getClass());
        codec.encode(writer, idValue, encoderContext);
    }

    private Object collectIdValues(final Object value) {
        if(value instanceof Collection) {
            List ids = new ArrayList(((Collection)value).size());
            for (Object o : (Collection)value) {
                ids.add(collectIdValues(o));
            }
            return ids;
        } else if(value instanceof Map) {
            final LinkedHashMap ids = new LinkedHashMap();
            Map<Object, Object> map = (Map<Object, Object>) value;
            for (final Map.Entry<Object, Object> o : map.entrySet()) {
                ids.put(o.getKey().toString(), collectIdValues(o.getValue()));
            }
            return ids;
        } else if (value.getClass().isArray()) {
            List ids = new ArrayList(((Object[])value).length);
            for (Object o : (Object[])value) {
                ids.add(collectIdValues(o));
            }
            return ids;
        } else {
            return encodeId(value);
        }

    }

    private Object encodeId(final Object value) {
        Object idValue;
        if(value instanceof Key) {
            idValue = ((Key)value).getId();
        } else {
            idValue = getIdField().getFieldValue(value);
        }
        if (!getField().getAnnotation(Reference.class).idOnly()) {
            if(idValue == null) {
                throw new MappingException("The ID value can not be null");
            }
            idValue = new DBRef(getFieldMappedClass().getCollectionName(), idValue);
        }
        return idValue;
    }

    private Object extractId(final Object o) {
        if(annotation.idOnly()) {
            return o;
        }
        return o instanceof DBRef ? ((DBRef) transformer.transform(o)).getId() : o;
    }
}

