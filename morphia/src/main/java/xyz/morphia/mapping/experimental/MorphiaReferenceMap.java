package xyz.morphia.mapping.experimental;

import com.mongodb.client.MongoCursor;
import xyz.morphia.Datastore;
import xyz.morphia.mapping.MappedClass;
import xyz.morphia.mapping.MappedField;
import xyz.morphia.mapping.Mapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @morphia.internal
 * @param <T>
 */
public class MorphiaReferenceMap<T> extends MorphiaReference<Map<String, T>> {
    private Map<String, Object> ids;
    private Map<String, T> values;

    /**
     * @morphia.internal
     */
    public MorphiaReferenceMap(final Datastore datastore, final MappedClass mappedClass, final Map ids) {
        super(datastore, mappedClass);
        this.ids = ids;
    }

    protected MorphiaReferenceMap(final Map<String, T> values, final String collection) {
        super(collection);
        set(values);
    }

    @SuppressWarnings("unchecked")
    public Map<String, T> get() {
        if (values == null && ids != null) {
            final MongoCursor<T> cursor = (MongoCursor<T>) getDatastore().find(getMappedClass().getClazz())
                                                                         .filter("_id in", ids.values())
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
        if(isResolved()) {
            final Class type = field.getTypeParameters().get(0).getSubClass();
            final Map<String, T> wrapped = get();
            Map ids = new LinkedHashMap(wrapped.size());
            for (final Entry<String, T> entry : wrapped.entrySet()) {
                ids.put(entry.getKey(), mapper.getId(entry.getValue()));
            }
            return mapper.toMongoObject(field, mapper.getMappedClass(type), ids);
        } else {
            return null;
        }
    }
}
