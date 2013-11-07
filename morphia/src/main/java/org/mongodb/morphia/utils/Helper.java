package org.mongodb.morphia.utils;


import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateOpsImpl;


/**
 * Exposes driver related DBObject stuff from Morphia objects
 *
 * @author scotthernandez
 */
public final class Helper {
    private Helper() {
    }

    public static DBObject getCriteria(final Query q) {
        return ((QueryImpl) q).getQueryObject();
    }

    public static DBObject getSort(final Query q) {
        return ((QueryImpl) q).getSortObject();
    }

    public static DBObject getFields(final Query q) {
        return ((QueryImpl) q).getFieldsObject();
    }

    public static DBCollection getCollection(final Query q) {
        return ((QueryImpl) q).getCollection();
    }

    public static DBCursor getCursor(final Iterable it) {
        return ((MorphiaIterator) it).getCursor();
    }

    public static DBObject getUpdateOperations(final UpdateOperations ops) {
        return ((UpdateOpsImpl) ops).getOps();
    }

    public static DB getDB(final Datastore ds) {
        return ds.getDB();
    }
}