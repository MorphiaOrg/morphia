package org.mongodb.morphia.query;


import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Const;
import org.mongodb.morphia.mapping.Mapper;


/**
 * @author Scott Hernandez
 */
public class MorphiaKeyIterator<T> extends MorphiaIterator<T, Key<T>> {
    public MorphiaKeyIterator(final DBCursor cursor, final Mapper m, final Class<T> clazz, final String kind) {
        super(cursor, m, clazz, kind, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Key<T> convertItem(final DBObject dbObj) {
        return new Key<T>(getClazz(), getCollection(), dbObj.get(Const.ID_KEY));
    }

}