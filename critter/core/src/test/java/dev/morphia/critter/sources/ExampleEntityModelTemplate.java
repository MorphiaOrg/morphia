package dev.morphia.critter.sources;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.internal.EntityBuilder;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.mapping.conventions.MorphiaDefaultsConvention;

public class ExampleEntityModelTemplate extends CritterEntityModel {

    public ExampleEntityModelTemplate(Mapper mapper) {
        super(mapper, Example.class);
        addProperty(new ExampleNamePropertyModelTemplate(this));
        addProperty(new ExampleAgePropertyModelTemplate(this));
        addProperty(new ExampleSalaryPropertyModelTemplate(this));
    }

    @Override
    public Set<Class<?>> classHierarchy() {
        return Set.of();
    }

    @Override
    public String collectionName() {
        return mapper.getConfig().collectionNaming().apply("Example");
    }

    @Override
    public List<PropertyModel> getShardKeys() {
        return List.of();
    }

    @Override
    public String discriminator() {
        return mapper.getConfig().discriminator().apply(Example.class, ".");
    }

    @Override
    public String discriminatorKey() {
        return MorphiaDefaultsConvention.applyDefaults(".", mapper.getConfig().discriminatorKey());
    }

    @Override
    public Entity getEntityAnnotation() {
        if (entityAnnotation == null) {
            entityAnnotation = EntityBuilder
                    .entityBuilder(Example.class.getAnnotation(Entity.class))
                    .build();
        }
        return entityAnnotation;
    }

    @Override
    public PropertyModel getIdProperty() {
        return null;
    }

    @Override
    public MorphiaInstanceCreator getInstanceCreator() {
        return null;
    }

    @Override
    public List<PropertyModel> getProperties(Class<? extends Annotation> type) {
        return List.of();
    }

    @Override
    public List<PropertyModel> getProperties() {
        return List.of();
    }

    @Override
    public Set<EntityModel> getSubtypes() {
        return Set.of();
    }

    @Override
    public void addSubtype(EntityModel subtype) {

    }

    @Override
    public EntityModel getSuperClass() {
        return null;
    }

    @Override
    public Class<?> getType() {
        return Example.class;
    }

    @Override
    public TypeData<?> getTypeData(Class<?> type, TypeData<?> suggested, Type genericType) {
        return null;
    }

    @Override
    public PropertyModel getVersionProperty() {
        return null;
    }

    @Override
    public boolean hasLifecycle(Class<? extends Annotation> type) {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean useDiscriminator() {
        return true;
    }
}
