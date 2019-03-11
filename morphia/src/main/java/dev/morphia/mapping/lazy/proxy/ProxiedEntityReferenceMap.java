package dev.morphia.mapping.lazy.proxy;


import dev.morphia.Key;

import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ProxiedEntityReferenceMap extends ProxiedReference {
    /**
     * @return the reference map
     */
    //CHECKSTYLE:OFF
    Map<Object, Key<?>> __getReferenceMap();
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    void __put(Object key, Key<?> referenceKey);
    //CHECKSTYLE:ON
}
