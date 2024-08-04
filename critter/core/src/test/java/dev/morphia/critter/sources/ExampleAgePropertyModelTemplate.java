package dev.morphia.critter.sources;

import java.lang.annotation.Annotation;
import java.util.List;

import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.bson.codecs.pojo.PropertyAccessor;

public class ExampleAgePropertyModelTemplate extends CritterPropertyModel {

    private PropertyAccessor<?> accessor = new ExampleAgeAccessorTemplate();

    public ExampleAgePropertyModelTemplate(EntityModel entityModel) {
        super(entityModel);

    }

    @Override
    public PropertyAccessor<Object> getAccessor() {
        return (PropertyAccessor<Object>) accessor;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return List.of();
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public String getFullName() {
        return "dev.morphia.critter.sources.Example#age";
    }

    @Override
    public List<String> getLoadNames() {
        return List.of();
    }

    @Override
    public String getMappedName() {
        return "age";
    }

    @Override
    public String getName() {
        return "age";
    }

    @Override
    public Class<?> getNormalizedType() {
        return int.class;
    }

    @Override
    public Class<?> getType() {
        return int.class;
    }

    @Override
    public TypeData<?> getTypeData() {
        return TypeData.get(int.class);
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isMap() {
        return false;
    }

    @Override
    public boolean isMultipleValues() {
        return false;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean isSet() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }
}
