package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.references.ReferenceCodec;
import dev.morphia.mapping.lazy.proxy.LazyReferenceFetchingException;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * @param <C>
 * @morphia.internal
 */
public abstract class CollectionReference<C extends Collection> extends MorphiaReference<C> {
    private MappedClass mappedClass;
    private List ids;
    private Map<String, List<Object>> collections = new HashMap<>();

    CollectionReference(final Datastore datastore, final MappedClass mappedClass, final List ids) {
        super(datastore);
        this.mappedClass = mappedClass;
        if (ids != null) {
            for (final Object o : ids) {
                collate(mappedClass, collections, o);
            }
        }

        this.ids = ids;
    }

    protected CollectionReference() {
    }

    @Override
    public List getIds() {
        return ids;
    }

    @Override
    public Class<C> getType() {
        return (Class<C>) mappedClass.getType();
    }

    static void collate(final MappedClass valueType, final Map<String, List<Object>> collections,
                        final Object o) {
        final String collectionName;
        final Object id;
        if (o instanceof DBRef) {
            final DBRef dbRef = (DBRef) o;
            collectionName = dbRef.getCollectionName();
            id = dbRef.getId();
        } else {
            collectionName = valueType.getCollectionName();
            id = o;
        }

        register(collections, collectionName).add(id);
    }

    static List register(final Map<String, List<Object>> collections, final String name) {
        return collections.computeIfAbsent(name, k -> new ArrayList<>());
    }

    abstract Collection<?> getValues();

    /**
     * {@inheritDoc}
     */
    public abstract C get();

    @Override
    final List<Object> getId(final Mapper mapper, final MappedClass mappedClass) {
        if(ids == null) {
            ids = getValues().stream()
                             .map(v -> ReferenceCodec.encodeId(mapper, mappedClass, v))
                             .collect(Collectors.toList());
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    final List<?> find() {
        final List<Object> values = new ArrayList(asList(new Object[ids.size()]));
        for (final Entry<String, List<Object>> entry : collections.entrySet()) {
            query(entry.getKey(), entry.getValue(), values);
        }
        resolve();
        return values.stream().filter(o -> o != null).collect(Collectors.toList());
    }

    void query(final String collection, final List<Object> collectionIds, final List<Object> values) {

        try (MongoCursor<?> cursor = ((AdvancedDatastore) getDatastore()).find(collection)
                                                                         .disableValidation()
                                                                         .filter("_id in ", collectionIds)
                                                                         .execute()) {
            final Map<Object, Object> idMap = new HashMap<>();
            while (cursor.hasNext()) {
                final Object entity = cursor.next();
                idMap.put(getDatastore().getMapper().getId(entity), entity);
            }

            if(!ignoreMissing() && idMap.size() != collectionIds.size()) {
                throw new LazyReferenceFetchingException(
                    Sofia.missingReferencedEntities(mappedClass.getType().getSimpleName()));

            }

            for (int i = 0; i < ids.size(); i++) {
                final Object id = ids.get(i);
                final Object value = idMap.get(id instanceof DBRef ? ((DBRef) id).getId() : id);
                if (value != null) {
                    values.set(i, value);
                }
            }
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
}
