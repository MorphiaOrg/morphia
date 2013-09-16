package org.mongodb.morphia.query;

import org.mongodb.morphia.Datastore;
import com.mongodb.DBCollection;

/**
 * An abstract implementation of {@link QueryFactory}.
 */
public abstract class AbstractQueryFactory implements QueryFactory {

  /**
   * @see #createQuery(Datastore, DBCollection, Class, com.mongodb.DBObject)
   */
  public <T> Query<T> createQuery(Datastore datastore, DBCollection collection, Class<T> type) {
    return createQuery(datastore, collection, type, null);
  }
}
