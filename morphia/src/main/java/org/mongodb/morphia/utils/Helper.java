package org.mongodb.morphia.utils;


import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateOpsImpl;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


/**
 * Exposes driver related DBObject stuff from Morphia objects
 *
 * @author scotthernandez
 */
@SuppressWarnings("rawtypes")
public class Helper {
  public static DBObject getCriteria(final Query q) {
    final QueryImpl qi = (QueryImpl) q;
    return qi.getQueryObject();
  }

  public static DBObject getSort(final Query q) {
    final QueryImpl qi = (QueryImpl) q;
    return qi.getSortObject();
  }

  public static DBObject getFields(final Query q) {
    final QueryImpl qi = (QueryImpl) q;
    return qi.getFieldsObject();
  }

  public static DBCollection getCollection(final Query q) {
    final QueryImpl qi = (QueryImpl) q;
    return qi.getCollection();
  }

  public static DBCursor getCursor(final Iterable it) {
    return ((MorphiaIterator) it).getCursor();
  }

  public static DBObject getUpdateOperations(final UpdateOperations ops) {
    final UpdateOpsImpl uo = (UpdateOpsImpl) ops;
    return uo.getOps();
  }

  public static DB getDB(final Datastore ds) {
    return ds.getDB();
  }
}