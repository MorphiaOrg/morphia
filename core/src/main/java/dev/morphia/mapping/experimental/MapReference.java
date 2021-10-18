package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.references.ReferenceCodec;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import static dev.morphia.query.experimental.filters.Filters.in;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MapReference<T> extends MorphiaReference<Map<Object, T>> {
    private Map<String, Object> ids;
    private Map<Object, T> values;
    private final Map<String, List<Object>> collections = new HashMap<>();

    /**
     * @param datastore   the datastore to use
     * @param mapper      the mapper to use
     * @param ids         the IDs of the entities
     * @param entityModel the model of the entity type
     * @morphia.internal
     */
    public MapReference(Datastore datastore, Mapper mapper, Map<String, Object> ids, EntityModel entityModel) {
        super(datastore, mapper);
        for (Entry<String, Object> entry : ids.entrySet()) {
            CollectionReference.collate(entityModel, collections, entry.getValue());
        }
        this.ids = ids;
    }

    private void setValues(Map<String, Object> values) {
        resolve();
    }

    MapReference(Map<Object, T> values) {
        this.values = values;
    }

    /**
     * Decodes a document in to entities
     *
     * @param datastore the datastore
     * @param mapper    the mapper
     * @param property  the property model
     * @param document  the Document to decode
     * @return the entities
     */
    public static MapReference decode(Datastore datastore, Mapper mapper, PropertyModel property,
                                      Document document) {
        final Class subType = property.getTypeData().getTypeParameters().get(0).getType();

        final Map<String, Object> ids = (Map<String, Object>) property.getDocumentValue(document);
        MapReference reference = null;
        if (ids != null) {
            reference = new MapReference(datastore, mapper, ids, mapper.getEntityModel(subType));
        }

        return reference;
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, T> get() {
        if (values == null && ids != null) {
            values = new LinkedHashMap<>();
            mergeReads();
        }
        return values;
    }

    @Override
    public Class<Map<Object, T>> getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> getIds() {
        List<Object> ids = new ArrayList<>(this.ids.values());
        if (!ids.isEmpty() && ids.get(0) instanceof DBRef) {
            ListIterator iterator = ids.listIterator();
            while (iterator.hasNext()) {
                iterator.set(((DBRef) iterator.next()).getId());
            }
        }

        return ids;
    }

    @Override
    public Map<String, Object> getId(Mapper mapper, EntityModel field) {
        if (ids == null) {
            ids = new LinkedHashMap<>();
            values.entrySet().stream()
                  .forEach(e -> ids.put(e.getKey().toString(),
                      ReferenceCodec.encodeId(mapper, e.getValue(), field)));
        }
        return ids;
    }

    private void mergeReads() {
        for (Entry<String, List<Object>> entry : collections.entrySet()) {
            readFromSingleCollection(entry.getKey(), entry.getValue());
        }
        resolve();
    }

    @SuppressWarnings("unchecked")
    private void readFromSingleCollection(String collection, List<Object> collectionIds) {

        try (MongoCursor<T> cursor = (MongoCursor<T>) getDatastore().find(collection)
                                                                    .filter(in("_id", collectionIds)).iterator()) {
            final Map<Object, T> idMap = new HashMap<>();
            while (cursor.hasNext()) {
                final T entity = cursor.next();
                idMap.put(getDatastore().getMapper().getId(entity), entity);
            }

            for (Entry<String, Object> entry : ids.entrySet()) {
                final Object id = entry.getValue();
                final T value = idMap.get(id instanceof DBRef ? ((DBRef) id).getId() : id);
                if (value != null) {
                    values.put(entry.getKey(), value);
                }
            }
        }
    }

}
