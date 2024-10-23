package dev.morphia.critter.sources;

import org.bson.codecs.pojo.PropertyAccessor;

public class ExampleNameAccessorTemplate implements PropertyAccessor<String> {
    @Override
    public <S> String get(S entity) {
        return ((Example) entity).__readNameTemplate();
    }

    @Override
    public <S> void set(S entity, String value) {
        ((Example) entity).__writeNameTemplate(value);
    }
}
