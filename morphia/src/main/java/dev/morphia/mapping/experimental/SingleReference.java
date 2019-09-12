package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Query;
import org.bson.Document;

/**
 * @param <T>
 * @morphia.internal
 */
public class SingleReference<T> extends MorphiaReference<T> {
    private MappedClass mappedClass;
    private Object id;
    private T value;

    /**
     * @morphia.internal
     */
    SingleReference(final Datastore datastore, final MappedClass mappedClass, final Object id) {
        super(datastore);
        this.mappedClass = mappedClass;
        this.id = id;
    }

    SingleReference(final T value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T get() {
        if (value == null && id != null) {
            value = (T) buildQuery().execute().tryNext();
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
            query = ((AdvancedDatastore) getDatastore()).find(mappedClass.getType())
                                                        .filter("_id", id);
        }
        return query;
    }

    Object getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isResolved() {
        return value != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object encode(final Mapper mapper, final Object value, final MappedField optionalExtraInfo) {
        if (isResolved()) {
            return wrapId(mapper, optionalExtraInfo, get());
        } else {
            return null;
        }

    }

    /**
     * Decodes a document in to an entity
     * @param datastore the datastore
     * @param mapper the mapper
     * @param mappedField the MappedField
     * @param paramType the type of the underlying entity
     * @param document the Document to decode
     * @return the entity
     */
    public static MorphiaReference<?> decode(final Datastore datastore,
                                             final Mapper mapper,
                                             final MappedField mappedField,
                                             final Class paramType, final Document document) {
        final MappedClass mappedClass = mapper.getMappedClass(paramType);
        Object id = document.get(mappedField.getMappedFieldName());

        return new SingleReference(datastore, mappedClass, id);
    }

}
