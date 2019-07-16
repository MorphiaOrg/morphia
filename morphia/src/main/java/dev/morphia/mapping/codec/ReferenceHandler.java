package dev.morphia.mapping.codec;

import com.mongodb.DBRef;
import com.mongodb.DocumentToDBRefTransformer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.MappingException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ReferenceHandler extends PropertyHandler {

    private final Reference annotation;
    private MappedField idField;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
    private DocumentToDBRefTransformer transformer = new DocumentToDBRefTransformer();

    public ReferenceHandler(final Datastore datastore, final Field field, final String name, final TypeData typeData) {
        super(datastore, field, name, typeData);
        annotation = field.getAnnotation(Reference.class);
    }

    private MappedField getIdField() {
        if(idField == null) {
            idField = getFieldMappedClass().getIdField();
        }
        return idField;
    }

    @Override
    public <T, S> S decodeProperty(final BsonReader reader,
                                   final DecoderContext decoderContext,
                                   final InstanceCreator<T> instanceCreator,
                                   final String name,
                                   final PropertyModel<S> propertyModel) {

        return (S) getDatastore().getMapper().getCodecRegistry()
                                 .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                                 .decode(reader, decoderContext);
    }

    @Override
    public <S, E> void set(final E instance, final PropertyModel<S> propertyModel, final S value, final Datastore datastore,
                           final Map<Object, Object> entityCache) {
        S fetched = value;
        if(value instanceof List) {
            List<Object> ids = new ArrayList<>();
            for (Object o : (List) value) {
                ids.add(extractId(o));
            }
            final MongoCollection<S> collection = datastore.getCollection((Class<S>) getFieldMappedClass().getClazz());
            final List<S> entities = collection.find(Filters.in("_id", ids)).into(new ArrayList<>());
            for (final S entity : entities) {
                entityCache.putIfAbsent(getIdField().getFieldValue(entity), entity);
            }
            List list = new ArrayList();
            for (Object id : ids) {
                final Object e = entityCache.get(id);
                if(e != null) {
                    list.add(e);
                }
            }
            if(list.size() < ids.size() && !annotation.ignoreMissing()) {
                throw new MappingException("Referenced entities not found");
            }
            fetched = (S) list;
        } else {
            final Object id = extractId(fetched);
            fetched = (S) entityCache.computeIfAbsent(id,
                (k) -> datastore.getCollection((Class<S>) getField().getType())
                                .find(Filters.eq("_id", id))
                                .first());
            if (fetched == null && !annotation.ignoreMissing()) {
                throw new MappingException("Referenced entity not found");
            }
        }
        propertyModel.getPropertyAccessor().set(instance, fetched);
    }

    @Override
    public <S, T> void encodeProperty(final BsonWriter writer,
                                      final T instance,
                                      final EncoderContext encoderContext,
                                      final PropertyModel<S> propertyModel) {
        S value = propertyModel.getPropertyAccessor().get(instance);
        if (propertyModel.shouldSerialize(value)) {
            if (value == null) {
                writer.writeNull(propertyModel.getReadName());
            } else {
                writer.writeName(propertyModel.getReadName());
                Object idValue = encodeValue(value);

                final Codec codec = getDatastore().getMapper().getCodecRegistry().get(idValue.getClass());
                codec.encode(writer, idValue, encoderContext);
            }
        }

    }

    @Override
    public <S> Object encodeValue(final S value) {
        return collectIdValues(value);
    }

    private Object collectIdValues(final Object value) {
        List ids;
        if(value instanceof Collection) {
            ids = new ArrayList(((Collection)value).size());
            for (Object o : (Collection)value) {
                ids.add(collectIdValues(o));
            }
        } else if (value.getClass().isArray()) {
            ids = new ArrayList(((Object[])value).length);
            for (Object o : (Object[])value) {
                ids.add(collectIdValues(o));
            }
        } else {
            return encodeId(value);
        }

        return ids;
    }

    private <S> Object encodeId(final S value) {
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
        return ((DBRef) transformer.transform(o)).getId();
    }
}

