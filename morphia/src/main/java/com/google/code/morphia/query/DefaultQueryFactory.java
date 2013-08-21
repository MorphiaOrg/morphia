package com.google.code.morphia.query;

import com.google.code.morphia.Datastore;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * A default implementation of {@link QueryFactory}.
 */
public class DefaultQueryFactory extends AbstractQueryFactory {

  /**
   * Creates and returns a {@link QueryImpl}.
   * 
   * @see QueryImpl
   */
  public <T> Query<T> createQuery(Datastore datastore, 
      DBCollection collection, Class<T> type, DBObject query) {
    
    QueryImpl<T> item = new QueryImpl<T>(type, collection, datastore);
    
    if (query != null) {
      item.setQueryObject(query);
    }
    
    return item;
  }
}
