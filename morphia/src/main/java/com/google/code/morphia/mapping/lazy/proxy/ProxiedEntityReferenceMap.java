package com.google.code.morphia.mapping.lazy.proxy;


import java.util.Map;

import com.google.code.morphia.Key;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ProxiedEntityReferenceMap extends ProxiedReference {

  void __put(Object key, Key<?> referenceKey);

  Map<Object, Key<?>> __getReferenceMap();
}
