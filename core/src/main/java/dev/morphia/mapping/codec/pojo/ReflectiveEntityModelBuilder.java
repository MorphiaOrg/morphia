package dev.morphia.mapping.codec.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.conventions.ConfigureProperties;
import dev.morphia.mapping.conventions.FieldDiscovery;
import dev.morphia.mapping.conventions.MethodDiscovery;
import dev.morphia.mapping.conventions.MorphiaConvention;
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention;
import dev.morphia.sofia.Sofia;

import static dev.morphia.mapping.PropertyDiscovery.FIELDS;

/**
 * Builds an {@link EntityModel} with reflection: collects the type's annotations, then runs the mapper's conventions,
 * which discover and configure its properties.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class ReflectiveEntityModelBuilder {
    private final Mapper mapper;
    private final Class<?> type;

    /**
     * @param mapper the mapper whose configuration and conventions to use
     * @param type   the type to model; for an {@code @ExternalEntity}, the stand-in carrying the mapping annotations
     */
    public ReflectiveEntityModelBuilder(Mapper mapper, Class<?> type) {
        this.mapper = mapper;
        this.type = type;
    }

    /**
     * @return the new model
     */
    public EntityModel build() {
        EntityModel model = new EntityModel(type);
        ClassHierarchy.annotations(type).values().forEach(model::annotation);

        List<MorphiaConvention> conventions = new ArrayList<>(List.of(
                new MorphiaDefaultsConvention(),
                mapper.getConfig().propertyDiscovery() == FIELDS ? new FieldDiscovery() : new MethodDiscovery(),
                new ConfigureProperties()));
        conventions.addAll(mapper.getConventions());
        for (MorphiaConvention convention : conventions) {
            convention.apply(mapper, model);
        }

        if (model.useDiscriminator()) {
            Objects.requireNonNull(model.discriminatorKey(), Sofia.notNull("discriminatorKey"));
            Objects.requireNonNull(model.discriminator(), Sofia.notNull("discriminator"));
        }
        model.collectionName(collectionName(model));
        model.initializeListeners();

        ExternalEntity externalEntity = model.getAnnotation(ExternalEntity.class);
        if (externalEntity != null) {
            model.setType(externalEntity.target());
        }
        return model;
    }

    private String collectionName(EntityModel model) {
        Entity entity = model.getAnnotation(Entity.class);
        return entity != null && !entity.value().equals(Mapper.IGNORED_FIELDNAME)
                ? entity.value()
                : mapper.getConfig().collectionNaming().apply(type.getSimpleName());
    }
}
