package org.mongodb.morphia.mapping.lazy.proxy;


import java.util.Map;

import org.mongodb.morphia.Key;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ProxiedEntityReferenceMap extends ProxiedReference {

  void __put(Object key, Key<?> referenceKey);

  Map<Object, Key<?>> __getReferenceMap();
}
