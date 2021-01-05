package dev.morphia.mapping.experimental;

import com.mongodb.DBRef;
import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.query.Query;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class SingleReference<T> extends MorphiaReference<T> {
    private EntityModel entityModel;
    private Object id;
    private T value;

    /**
     * @param datastore   the datastore to use
     * @param entityModel the entity's mapped class
     * @param id          the ID value
     * @morphia.internal
     */
    public SingleReference(Datastore datastore, EntityModel entityModel, Object id) {
        super(datastore);
        this.entityModel = entityModel;
        this.id = id;
        if (entityModel.getType().isInstance(id)) {
            value = (T) id;
            this.id = entityModel.getIdField().getValue(value);
            resolve();
        }

    }

    SingleReference(T value) {
        this.value = value;
    }

    /**
     * Decodes a document in to an entity
     *
     * @param datastore   the datastore
     * @param mapper      the mapper
     * @param mappedField the MappedField
     * @param paramType   the type of the underlying entity
     * @param document    the Document to decode
     * @return the entity
     */
    public static MorphiaReference<?> decode(Datastore datastore,
                                             Mapper mapper,
                                             FieldModel mappedField,
                                             Class<?> paramType, Document document) {
        final EntityModel entityModel = mapper.getEntityModel(paramType);
        Object id = document.get(mappedField.getMappedName());

        return new SingleReference<>(datastore, entityModel, id);
    }

    @Override
    public Object encode(Mapper mapper, Object value, FieldModel optionalExtraInfo) {
        if (isResolved()) {
            return wrapId(mapper, optionalExtraInfo, get());
        } else {
            return null;
        }

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
    public Class<T> getType() {
        return (Class<T>) entityModel.getType();
    }

    @Override
    public List<Object> getIds() {
        return List.of(getId());
    }

    @Override
    Object getId(Mapper mapper, Datastore datastore, EntityModel fieldClass) {
        if (id == null) {
            EntityModel entityModel = getEntityModel(mapper);
            id = entityModel.getIdField().getValue(get());
            if (!entityModel.equals(fieldClass)) {
                id = new DBRef(entityModel.getCollectionName(), id);
            }
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

    EntityModel getEntityModel(Mapper mapper) {
        if (entityModel == null) {
            entityModel = mapper.getEntityModel(get().getClass());
        }

        return entityModel;
    }

}
