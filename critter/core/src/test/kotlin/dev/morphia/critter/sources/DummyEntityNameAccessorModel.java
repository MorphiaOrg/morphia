package dev.morphia.critter.sources;

import org.bson.codecs.pojo.PropertyAccessor;

public class DummyEntityNameAccessorModel implements PropertyAccessor<String> {
    @Override
    public <S> String get(S entity) {
        return ((DummyEntity) entity).__readNameSample();
    }

    @Override
    public <S> void set(S entity, String value) {
        ((DummyEntity) entity).__writeNameSample(value);
    }
}
