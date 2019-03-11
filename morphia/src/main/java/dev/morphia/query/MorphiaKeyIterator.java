package dev.morphia.query;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.mapping.Mapper;

/**
 * Defines an Iterator across the Key values for a given type.
 *
 * @param <T> the entity type
 * @author Scott Hernandez
 * @deprecated this is being replaced by {@link MongoCursor}
 */
@Deprecated
public class MorphiaKeyIterator<T> extends MorphiaIterator<T, Key<T>> {
    /**
     * Create
     * @param datastore  the Datastore to use when fetching this reference
     * @param cursor     the cursor to use
     * @param mapper     the Mapper to use
     * @param clazz      the original type being iterated
     * @param collection the mongodb collection
     */
    public MorphiaKeyIterator(final Datastore datastore, final DBCursor cursor, final Mapper mapper,
                              final Class<T> clazz, final String collection) {
        super(datastore, cursor, mapper, clazz, collection, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Key<T> convertItem(final DBObject dbObj) {
        Object id = dbObj.get("_id");
        if (id instanceof DBObject) {
            Class type = getMapper().getMappedClass(getClazz()).getMappedIdField().getType();
            id = getMapper().fromDBObject(getDatastore(), type, (DBObject) id, getMapper().createEntityCache());
        }
        return new Key<T>(getClazz(), getCollection(), id);
    }
}
