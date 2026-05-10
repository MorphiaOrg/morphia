package dev.morphia.mapping.codec.references;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

import static dev.morphia.query.filters.Filters.eq;

/**
 * @param <T>
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings({ "rawtypes", "unchecked" })
class MapReference<T> extends LazyReference<Map<Object, T>> {
    private Map<String, Object> ids;

    private EntityModel entityModel;

    private Map<Object, T> values;

    MapReference(MorphiaDatastore datastore, Map<String, Object> ids, EntityModel entityModel) {
        super(datastore);
        this.ids = ids;
        this.entityModel = entityModel;
    }

    MapReference(MorphiaDatastore datastore, Map<Object, T> values) {
        super(datastore);
        this.values = values;
    }

    @Override
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
        for (Entry<String, Object> entry : ids.entrySet()) {
            DBRef id = normalizeId(entry.getValue());
            try (MongoCursor<T> cursor = (MongoCursor<T>) getDatastore().find(id.getCollectionName())
                    .filter(eq("_id", id.getId())).iterator()) {
                values.put(entry.getKey(), cursor.next());
            }
        }

        resolve();
    }

    private DBRef normalizeId(Object value) {
        return value instanceof DBRef
                ? (DBRef) value
                : new DBRef(entityModel.collectionName(), value);
    }
}
