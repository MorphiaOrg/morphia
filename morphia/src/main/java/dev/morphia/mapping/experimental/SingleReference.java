package dev.morphia.mapping.experimental;

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Query;

/**
 * @morphia.internal
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class SingleReference<T> extends MorphiaReference<T> {
    private Object id;
    private T value;

    /**
     * @morphia.internal
     */
    SingleReference(final Datastore datastore, final MappedClass mappedClass, final Object id) {
        super(datastore, mappedClass);
        this.id = id;
    }

    SingleReference(final T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (value == null && id != null) {
            value = (T) buildQuery().find().tryNext();
        }
        return value;
    }

    Query<?> buildQuery() {
        final Query<?> query;
        if (id instanceof DBRef) {
            final Class<?> clazz = getDatastore()
                                       .getMapper()
                                       .getClassFromCollection(((DBRef) id).getCollectionName());
            query = ((AdvancedDatastore) getDatastore()).find(clazz)
                                                        .filter("_id", ((DBRef) id).getId());
        } else {
            query = ((AdvancedDatastore) getDatastore()).find(getMappedClass().getClazz())
                                                        .filter("_id", id);
        }
        return query;
    }

    Object getId() {
        return id;
    }

    public boolean isResolved() {
        return value != null;
    }

    @Override
    public Object encode(Mapper mapper, final Object value, final MappedField optionalExtraInfo) {
        if(isResolved()) {
            return wrapId(mapper, optionalExtraInfo, get());
        } else {
            return null;
        }

    }

    public static MorphiaReference<?> decode(final Datastore datastore,
                                             final Mapper mapper,
                                             final MappedField mappedField,
                                             final Class paramType, final DBObject dbObject) {
        final MappedClass mappedClass = mapper.getMappedClass(paramType);
        Object id = dbObject.get(mappedField.getMappedFieldName());

        return new SingleReference(datastore, mappedClass, id);
    }

}
