package org.mongodb.morphia.mapping.lazy.proxy;


import org.mongodb.morphia.Key;

import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ProxiedEntityReferenceMap extends ProxiedReference {
    //CHECKSTYLE:OFF
    void __put(Object key, Key<?> referenceKey);

    Map<Object, Key<?>> __getReferenceMap();
    //CHECKSTYLE:ON
}
