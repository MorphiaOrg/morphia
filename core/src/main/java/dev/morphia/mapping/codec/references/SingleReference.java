package dev.morphia.mapping.codec.references;

import java.util.List;

import com.mongodb.DBRef;
import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.Query;
import dev.morphia.sofia.Sofia;

import static dev.morphia.query.filters.Filters.eq;

/**
 * @param <T>
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
class SingleReference<T> extends LazyReference<T> {
    private EntityModel entityModel;
    private Object id;
    private T value;

    SingleReference(MorphiaDatastore datastore, EntityModel entityModel, Object id) {
        super(datastore);
        this.entityModel = entityModel;
        this.id = id;
        if (entityModel.getType().isInstance(id)) {
            value = (T) id;
            PropertyModel idProperty = entityModel.getIdProperty();
            if (idProperty != null) {
                this.id = idProperty.getValue(value);
                resolve();
            } else {
                throw new MappingException(Sofia.noIdPropertyFound(entityModel.getType().getName()));
            }
        }
    }

    SingleReference(MorphiaDatastore datastore, T value) {
        super(datastore);
        this.value = value;
    }

    @Override
    public T get() {
        if (!isResolved() && value == null && id != null) {
            value = (T) buildQuery().iterator().tryNext();
            if (value == null && !ignoreMissing()) {
                throw new ReferenceException(
                        Sofia.missingReferencedEntity(entityModel.getType().getSimpleName()));
            }
            resolve();
        }
        return value;
    }

    @Override
    public List<Object> getIds() {
        return List.of(getId());
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) entityModel.getType();
    }

    @Override
    Object getId(Mapper mapper, EntityModel fieldClass) {
        if (id == null) {
            EntityModel entityModel = getEntityModel(mapper);
            if (entityModel != null && entityModel.getIdProperty() != null) {
                id = entityModel.getIdProperty().getValue(get());
                if (!entityModel.equals(fieldClass)) {
                    id = new DBRef(entityModel.collectionName(), id);
                }
            }
        }
        if (id == null) {
            throw new ReferenceException(Sofia.noIdForReference());
        }
        return id;
    }

    private Object getId() {
        return id instanceof DBRef ? ((DBRef) id).getId() : id;
    }

    Query<?> buildQuery() {
        final Query<?> query;
        if (id instanceof DBRef) {
            query = getDatastore().find(getDatastore()
                    .getMapper()
                    .getClassFromCollection(((DBRef) this.id).getCollectionName()));
        } else {
            query = getDatastore().find(entityModel.getType());
        }
        return query.filter(eq("_id", getId()));
    }

    @Nullable
    EntityModel getEntityModel(Mapper mapper) {
        if (entityModel == null) {
            T t = get();
            if (t != null) {
                entityModel = mapper.getEntityModel(t.getClass());
            }
        }

        return entityModel;
    }
}
