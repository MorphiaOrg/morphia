package dev.morphia.critter.sources;

import java.util.List;

import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.bson.codecs.pojo.PropertyAccessor;

public class ExampleNamePropertyModelTemplate extends CritterPropertyModel {

    private PropertyAccessor<?> accessor = new ExampleNameAccessorTemplate();

    public ExampleNamePropertyModelTemplate(EntityModel entityModel) {
        super(entityModel);
        /*
         * annotation(propertyBuilder()
         * .value("myName")
         * .concreteClass(String.class)
         * .build());
         * annotation(alsoLoadBuilder()
         * .value("name1", "name2")
         * .build());
         */
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
        return "dev.morphia.critter.sources.Example#name";
    }

    @Override
    public List<String> getLoadNames() {
        return List.of("name1", "name2");
    }

    @Override
    public String getMappedName() {
        return "myName";
    }

    @Override
    public String getName() {
        return "name";
    }

    @Override
    public Class<?> getNormalizedType() {
        return String.class;
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

    @Override
    public TypeData<?> getTypeData() {
        return TypeData.get(String.class);
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
