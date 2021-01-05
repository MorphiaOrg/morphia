package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.references.ReferenceCodec;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.sofia.Sofia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import static dev.morphia.query.experimental.filters.Filters.in;
import static java.util.Arrays.asList;

/**
 * @param <C>
 * @morphia.internal
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class CollectionReference<C extends Collection> extends MorphiaReference<C> {
    private EntityModel entityModel;
    private List ids;
    private final Map<String, List<Object>> collections = new HashMap<>();

    CollectionReference(Datastore datastore, EntityModel entityModel, List ids) {
        super(datastore);
        this.entityModel = entityModel;
        if (ids != null) {
            if (ids.stream().allMatch(entityModel.getType()::isInstance)) {
                setValues(ids);
            } else {
                for (Object o : ids) {
                    collate(entityModel, collections, o);
                }
                this.ids = ids;
            }
        }
    }

    abstract void setValues(List ids);

    protected CollectionReference() {
    }

    static void collate(EntityModel valueType, Map<String, List<Object>> collections,
                        Object o) {
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

    static List register(Map<String, List<Object>> collections, String name) {
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
        return (Class<C>) entityModel.getType();
    }

    @Override
    public List<Object> getIds() {
        List<Object> ids = new ArrayList<>(this.ids);
        if (!ids.isEmpty() && ids.get(0) instanceof DBRef) {
            ListIterator iterator = ids.listIterator();
            while (iterator.hasNext()) {
                iterator.set(((DBRef) iterator.next()).getId());
            }
        }
        return ids;
    }

    @Override
    public Object encode(Mapper mapper, Object value, FieldModel field) {
        if (isResolved()) {
            List ids = new ArrayList();
            for (Object entity : get()) {
                ids.add(wrapId(mapper, field, entity));
            }
            return ids;
        } else {
            return null;
        }
    }

    @Override
    final List<Object> getId(Mapper mapper, Datastore datastore, EntityModel entityModel) {
        if (ids == null) {
            ids = getValues().stream()
                             .map(v -> ReferenceCodec.encodeId(mapper, datastore, v, entityModel))
                             .collect(Collectors.toList());
        }
        return ids;
    }

    private List<Object> extractIds(List<Object> list) {
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

    private List<Object> mapIds(List list, Map<Object, Object> idMap) {
        final List<Object> values = new ArrayList<>(asList(new Object[list.size()]));
        for (int i = 0; i < list.size(); i++) {
            final Object id = list.get(i);

            final Object value;
            if (id instanceof List) {
                value = mapIds((List) id, idMap);
            } else {
                value = idMap.get(id instanceof DBRef ? ((DBRef) id).getId() : id);
            }
            if (value != null) {
                values.set(i, value);
            }
        }

        return values;
    }

    final List find() {
        HashMap<Object, Object> idMap = new HashMap<>();
        for (Entry<String, List<Object>> entry : collections.entrySet()) {
            idMap.putAll(query(entry.getKey(), extractIds(entry.getValue())));
        }
        List values = mapIds(ids, idMap).stream()
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
        resolve();
        return values;
    }

    abstract Collection<?> getValues();

    Map<Object, Object> query(String collection, List<Object> collectionIds) {

        final Map<Object, Object> idMap = new HashMap<>();
        try (MongoCursor<?> cursor = getDatastore().find(collection)
                                                   .disableValidation()
                                                   .filter(in("_id", collectionIds)).iterator()) {
            while (cursor.hasNext()) {
                final Object entity = cursor.next();
                idMap.put(getDatastore().getMapper().getId(entity), entity);
            }

            if (!ignoreMissing() && idMap.size() != collectionIds.size()) {
                throw new ReferenceException(
                    Sofia.missingReferencedEntities(entityModel.getType().getSimpleName()));

            }
        }

        return idMap;
    }
}
