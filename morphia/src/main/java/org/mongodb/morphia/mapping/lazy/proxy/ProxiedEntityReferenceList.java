package org.mongodb.morphia.mapping.lazy.proxy;


import java.util.Collection;
import java.util.List;

import org.mongodb.morphia.Key;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ProxiedEntityReferenceList extends ProxiedReference {

  void __add(Key<?> key);

  void __addAll(Collection<? extends Key<?>> keys);

  List<Key<?>> __getKeysAsList();

}
