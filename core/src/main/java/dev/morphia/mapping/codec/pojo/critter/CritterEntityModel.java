package dev.morphia.mapping.codec.pojo.critter;

import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;

public abstract class CritterEntityModel extends EntityModel {
    private Entity entityAnnotation;

    public CritterEntityModel(Mapper mapper, Class<?> type) {
        super(mapper, type);
    }

    @Override
    public Entity getEntityAnnotation() {
        if (entityAnnotation == null) {
            entityAnnotation = entityAnnotation();
        }
        return entityAnnotation;
    }

    protected abstract Entity entityAnnotation();
}
