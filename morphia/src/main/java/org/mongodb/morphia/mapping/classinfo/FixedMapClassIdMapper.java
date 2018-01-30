package org.mongodb.morphia.mapping.classinfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple class id mapper which delegates to a provided hash map
 */
public class FixedMapClassIdMapper implements ClassIdMapper {

    private final Map<String, Class<?>> idToClass;
    private final Map<Class<?>, String> classToId;

    /**
     * @param classToId the mapping of class to id
     */
    public FixedMapClassIdMapper(final Map<Class<?>, String> classToId) {
        this.classToId = Collections.unmodifiableMap(new HashMap<Class<?>, String>(classToId));

        final Map<String, Class<?>> idToClass = new HashMap<String, Class<?>>();
        for (Map.Entry<Class<?>, String> entry : classToId.entrySet()) {
            idToClass.put(entry.getValue(), entry.getKey());
        }
        this.idToClass = Collections.unmodifiableMap(idToClass);
    }

    @Override
    public String getId(final Object value) {
        return classToId.get(value.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(final String id) {
        return (Class<T>) idToClass.get(id);
    }

    @Override
    public void setCaching(final boolean caching) {
        // no op
    }

    @Override
    public boolean isCaching() {
        return false;
    }
}
