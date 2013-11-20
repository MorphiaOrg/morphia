package org.mongodb.morphia.query;

import org.mongodb.morphia.Datastore;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * A factory for {@link Query}ies.
 */
public interface QueryFactory {

  /**
   * Creates and returns a {@link Query} for the given arguments. Default implementations of this 
   * method will simply delegate to {@link #createQuery(Datastore, DBCollection, Class, DBObject)}
   * with the last argument being {@code null}.
   * 
   * @see #createQuery(Datastore, DBCollection, Class, DBObject)
   */
  <T> Query<T> createQuery(Datastore datastore, DBCollection collection, Class<T> type);
  
  /**
   * Creates and returns a {@link Query} for the given arguments. The last argument is optional
   * and may be {@code null}.
   */
  <T> Query<T> createQuery(Datastore datastore, DBCollection collection, Class<T> type, DBObject query);

  /**
   * Creates an unvalidated {@link Query} typically for use in aggregation pipelines.
   */
  <T> Query<T> createQuery(Datastore datastore);
}
