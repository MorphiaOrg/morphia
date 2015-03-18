package org.mongodb.morphia.mapping.lazy;


import java.io.Serializable;

import org.mongodb.morphia.Datastore;


/**
 * Lightweight object to be created (hopefully by a factory some day) to create provide a Datastore-reference to a resolving Proxy. If this
 * was created by a common Object factory, it could make use of the current context (like Guice Scopes etc.)
 *
 * @author uwe schaefer
 * @see LazyProxyFactory
 */
public interface DatastoreProvider extends Serializable {
  Datastore get();

  void register(Datastore ds);
}
