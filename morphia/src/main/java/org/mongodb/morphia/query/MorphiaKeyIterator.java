package org.mongodb.morphia.query;


import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;


/**
 * Defines an Iterator across the Key values for a given type.
 *
 * @param <T> the entity type
 * @author Scott Hernandez
 */
public class MorphiaKeyIterator<T> extends MorphiaIterator<T, Key<T>> {
    /**
     * Create
     *
     * @param cursor     the cursor to use
     * @param mapper     the Mapper to use
     * @param clazz      the original type being iterated
     * @param collection the mongodb collection
     */
    public MorphiaKeyIterator(final DBCursor cursor, final Mapper mapper, final Class<T> clazz, final String collection) {
        super(cursor, mapper, clazz, collection, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Key<T> convertItem(final DBObject dbObj) {
        return new Key<T>(getClazz(), getCollection(), dbObj.get(Mapper.ID_KEY));
    }

}
