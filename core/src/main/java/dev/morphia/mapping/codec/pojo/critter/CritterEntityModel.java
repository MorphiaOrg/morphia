package dev.morphia.mapping.codec.pojo.critter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;

/**
 * 0
 * 
 * @hidden
 * @morphia.internal
 */
@SuppressWarnings("NullableProblems")
public abstract class CritterEntityModel extends EntityModel {
    protected final Mapper mapper;
    protected Entity entityAnnotation;

    public CritterEntityModel(Mapper mapper, Class<?> type) {
        super(type);
        this.mapper = mapper;
    }

    @Override
    public abstract Set<Class<?>> classHierarchy();

    @Override
    public final void discriminator(String discriminator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final EntityModel discriminatorKey(String discriminatorKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final EntityModel discriminatorEnabled(boolean discriminatorEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract String collectionName();

    @Override
    public final EntityModel collectionName(String collectionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract List<PropertyModel> getShardKeys();

    @Override
    public abstract String discriminator();

    @Override
    public abstract String discriminatorKey();

    @Override
    public abstract Entity getEntityAnnotation();

    @Override
    public abstract PropertyModel getIdProperty();

    @Override
    public final void setIdProperty(PropertyModel model) {

    }

    @Override
    public abstract MorphiaInstanceCreator getInstanceCreator();

    @Override
    public abstract List<PropertyModel> getProperties(Class<? extends Annotation> type);

    @Override
    public abstract List<PropertyModel> getProperties();

    @Override
    public final PropertyModel getProperty(String name) {
        return super.getProperty(name);
    }

    @Override
    public abstract EntityModel getSubtype(Class<?> type);

    @Override
    public abstract Set<EntityModel> getSubtypes();

    @Override
    public abstract void addSubtype(EntityModel subtype);

    @Override
    public abstract EntityModel getSuperClass();

    @Override
    public abstract Class<?> getType();

    @Override
    public final void setType(Class<?> type) {
    }

    @Override
    public abstract TypeData<?> getTypeData(Class<?> type, TypeData<?> suggested, Type genericType);

    @Override
    public abstract PropertyModel getVersionProperty();

    @Override
    public final void setVersionProperty(PropertyModel model) {

    }

    @Override
    public abstract boolean hasLifecycle(Class<? extends Annotation> type);

    @Override
    public abstract boolean isAbstract();

    @Override
    public abstract boolean isInterface();

    @Override
    public abstract boolean useDiscriminator();
}
