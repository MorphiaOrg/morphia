package dev.morphia.critter.sources;

import java.util.List;

import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.bson.codecs.pojo.PropertyAccessor;

import static dev.morphia.annotations.internal.PropertyBuilder.propertyBuilder;

public class ExampleSalaryPropertyModelTemplate extends CritterPropertyModel {

    private PropertyAccessor<?> accessor = new ExampleSalaryAccessorTemplate();

    public ExampleSalaryPropertyModelTemplate(EntityModel entityModel) {
        super(entityModel);
        annotation(propertyBuilder()
                .build());
    }

    @Override
    public PropertyAccessor<Object> getAccessor() {
        return (PropertyAccessor<Object>) accessor;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public String getFullName() {
        return "dev.morphia.critter.sources.Example#salary";
    }

    @Override
    public List<String> getLoadNames() {
        return List.of();
    }

    @Override
    public String getMappedName() {
        return "salary";
    }

    @Override
    public String getName() {
        return "salary";
    }

    @Override
    public Class<?> getNormalizedType() {
        return Long.class;
    }

    @Override
    public Class<?> getType() {
        return Long.class;
    }

    @Override
    public TypeData<?> getTypeData() {
        return TypeData.get(Long.class);
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
