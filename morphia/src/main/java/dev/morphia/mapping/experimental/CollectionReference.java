package dev.morphia.mapping.experimental;

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * @param <C>
 * @morphia.internal
 */
public abstract class CollectionReference<C extends Collection> extends MorphiaReference<C> {
    private List<Object> ids;
    private Map<String, List<Object>> collections = new HashMap<String, List<Object>>();

    CollectionReference() {
    }

    CollectionReference(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        super(datastore, mappedClass);
        List<Object> unwrapped = ids;
        if (ids != null) {
            for (final Object o : ids) {
                collate(datastore, collections, o);
            }
        }

        this.ids = unwrapped;
    }

    static void collate(final Datastore datastore, final Map<String, List<Object>> collections,
                        final Object o) {
        final String collectionName;
        final Object id;
        if (o instanceof DBRef) {
            final DBRef dbRef = (DBRef) o;
            collectionName = dbRef.getCollectionName();
            id = dbRef.getId();
        } else {
            collectionName = datastore.getMapper().getCollectionName(o);
            id = o;
        }

        register(collections, collectionName).add(id);
    }

    static List register(final Map<String, List<Object>> collections, final String name) {
        List<Object> list = collections.get(name);
        if (list == null) {
            list = new ArrayList<Object>();
            collections.put(name, list);
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isResolved() {
        return getValues() != null;
    }

    abstract Collection<?> getValues();

    /**
     * {@inheritDoc}
     */
    public abstract C get();

    final List<Object> getIds() {
        return ids;
    }

    @SuppressWarnings("unchecked")
    final List<?> find() {
        final List<Object> values = new ArrayList(asList(new Object[ids.size()]));
        for (final Entry<String, List<Object>> entry : collections.entrySet()) {
            query(entry.getKey(), entry.getValue(), values);
        }
        return values;
    }

    void query(final String collection, final List<Object> collectionIds, final List<Object> values) {

        final MongoCursor<?> cursor = ((AdvancedDatastore) getDatastore()).find(collection, Object.class)
                                                                          .disableValidation()
                                                                          .filter("_id in ", collectionIds)
                                                                          .find();
        try {
            final Map<Object, Object> idMap = new HashMap<Object, Object>();
            while (cursor.hasNext()) {
                final Object entity = cursor.next();
                idMap.put(getDatastore().getMapper().getId(entity), entity);
            }

            for (int i = 0; i < ids.size(); i++) {
                final Object id = ids.get(i);
                final Object value = idMap.get(id instanceof DBRef ? ((DBRef) id).getId() : id);
                if (value != null) {
                    values.set(i, value);
                }
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public Object encode(final Mapper mapper, final Object value, final MappedField field) {
        if (isResolved()) {
            List ids = new ArrayList();
            for (final Object entity : get()) {
                ids.add(wrapId(mapper, field, entity));
            }
            return ids;
        } else {
            return null;
        }
    }

    /**
     * Decodes a document in to entities
     * @param datastore the datastore
     * @param mapper the mapper
     * @param mappedField the MappedField
     * @param paramType the type of the underlying entity
     * @param dbObject the DBObject to decode
     * @return the entities
     */
    public static MorphiaReference<?> decode(final Datastore datastore,
                                             final Mapper mapper,
                                             final MappedField mappedField,
                                             final Class paramType,
                                             final DBObject dbObject) {
        MorphiaReference reference = null;
        final List dbVal = (List) mappedField.getDbObjectValue(dbObject);
        if (dbVal != null) {
            final Class subType = mappedField.getTypeParameters().get(0).getSubClass();
            final MappedClass mappedClass = mapper.getMappedClass(subType);

            if (Set.class.isAssignableFrom(paramType)) {
                reference = new SetReference(datastore, mappedClass, dbVal);
            } else {
                reference = new ListReference(datastore, mappedClass, dbVal);
            }
        }

        return reference;
    }
}
