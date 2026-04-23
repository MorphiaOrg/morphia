package dev.morphia.mapping.codec.pojo.critter;

import java.lang.annotation.Annotation;

import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;

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

    /**
     * Sets up {@link MorphiaPropertySerialization} on every property model.
     * Must be called at the end of the generated subclass constructor, after all
     * properties have been added via {@code addProperty()}.
     *
     * @param mapper the mapper whose configuration governs serialization behaviour
     */
    protected final void configureProperties(Mapper mapper) {
        for (PropertyModel property : getProperties()) {
            property.serialization(new MorphiaPropertySerialization(mapper.getConfig(), property));
        }
    }

    @Override
    public final EntityModel collectionName(String collectionName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void discriminator(String discriminator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final EntityModel discriminatorEnabled(boolean discriminatorEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final EntityModel discriminatorKey(String discriminatorKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract String collectionName();

    @Override
    public abstract String discriminator();

    @Override
    public abstract String discriminatorKey();

    @Override
    public abstract boolean hasLifecycle(Class<? extends Annotation> type);

    @Override
    public abstract boolean isAbstract();

    @Override
    public abstract boolean isInterface();

    @Override
    public abstract boolean useDiscriminator();
}
