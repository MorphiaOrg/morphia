package xyz.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;
import xyz.morphia.query.Query;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @param <T>
 * @morphia.internal
 */
public class MapReference<T> extends MorphiaReference<Map<String, T>> {
    private Map<String, Object> ids;
    private Map<String, T> values;

    /**
     * @morphia.internal
     */
    public MapReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final Map<String, Object> ids) {
        super(datastore, mappedClass, collection);
        this.ids = unwrap(ids, collection != null);
    }

    private Map<String, Object> unwrap(final Map<String, Object> ids, final boolean dbRefs) {
        if(ids != null && !ids.isEmpty()) {
            if(dbRefs) {
                for (final Entry<String, Object> entry : ids.entrySet()) {
                    ids.put(entry.getKey(), ((DBRef) entry.getValue()).getId());
                }
            }
        }

        return ids;
    }

    protected MapReference(final Map<String, T> values, final String collection) {
        super(collection);
        set(values);
    }

    @SuppressWarnings("unchecked")
    public Map<String, T> get() {
        if (values == null && ids != null) {
            final Query<?> query = buildQuery()
                                       .filter("_id in", ids.values());
            final MongoCursor<T> cursor = (MongoCursor<T>) query
                                                               .find();
            try {

                final Map<Object, T> idMap = new HashMap<Object, T>();
                while (cursor.hasNext()) {
                    final T entity = cursor.next();
                    idMap.put(getDatastore().getMapper().getId(entity), entity);
                }

                final LinkedHashMap lhm = new LinkedHashMap();
                for (final Entry<String, Object> entry : ids.entrySet()) {
                    lhm.put(entry.getKey(), idMap.get(entry.getValue()));
                }
                values = lhm;
            } finally {
                cursor.close();
            }
        }
        return values;
    }

    public void set(Map<String, T> values) {
        this.values = values;
    }

    public boolean isResolved() {
        return values != null;
    }

    @Override
    public Object encode(final Mapper mapper, final Object value, final MappedField field) {
        if (isResolved()) {
            final Class type = field.getTypeParameters().get(0).getSubClass();
            final Map<String, T> wrapped = get();
            Map<String, Object> ids = new LinkedHashMap<String, Object>(wrapped.size());
            for (final Entry<String, T> entry : wrapped.entrySet()) {
                ids.put(entry.getKey(), wrapId(mapper, entry.getValue()));
            }
            return mapper.toMongoObject(field, mapper.getMappedClass(type), ids);
        } else {
            return null;
        }
    }
}
