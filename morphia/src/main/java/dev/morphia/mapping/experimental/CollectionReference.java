package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.references.ReferenceCodec;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

    /**
     * Gets the referenced entities.  This may require at least one request to the server.
     *
     * @return the referenced entities
     */
    public abstract C get();

    @Override
    public Class<C> getType() {
        return (Class<C>) mappedClass.getType();
    }

    @Override
    public List getIds() {
        return ids;
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

    @Override
    final List<Object> getId(final Mapper mapper, final Datastore datastore, final MappedClass mappedClass) {
        if (ids == null) {
            ids = getValues().stream()
                             .map(v -> ReferenceCodec.encodeId(mapper, datastore, v, mappedClass))
                             .collect(Collectors.toList());
        }
        return ids;
    }

    private List<Object> extractIds(final List<Object> list) {
        List<Object> ids = new ArrayList<>();
        list.forEach(i -> {
            if (i instanceof List) {
                ids.addAll(extractIds((List<Object>) i));
            } else {
                ids.add(i);
            }
        });
        return ids;
    }

    private List<Object> mapIds(final List list, final Map<Object, Object> idMap) {
        final List<Object> values = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            final Object id = list.get(i);

            final Object value;
            if (id instanceof List) {
                value = mapIds((List) id, idMap);
            } else {
                value = idMap.get(id instanceof DBRef ? ((DBRef) id).getId() : id);
            }
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    final List find() {
        List values = new ArrayList();
        for (final Entry<String, List<Object>> entry : collections.entrySet()) {
            values.addAll(query(entry.getKey(), extractIds(entry.getValue())));
        }
        resolve();
        return values;
    }

    List<Object> query(final String collection, final List<Object> collectionIds) {

        try (MongoCursor<?> cursor = ((AdvancedDatastore) getDatastore()).find(collection)
                                                                         .disableValidation()
                                                                         .filter("_id in ", collectionIds)
                                                                         .execute()) {
            final Map<Object, Object> idMap = new HashMap<>();
            while (cursor.hasNext()) {
                final Object entity = cursor.next();
                idMap.put(getDatastore().getMapper().getId(entity), entity);
            }

            if (!ignoreMissing() && idMap.size() != collectionIds.size()) {
                throw new ReferenceException(
                    Sofia.missingReferencedEntities(mappedClass.getType().getSimpleName()));

            }

            return mapIds(ids, idMap);
        }
    }

    abstract Collection<?> getValues();
}
