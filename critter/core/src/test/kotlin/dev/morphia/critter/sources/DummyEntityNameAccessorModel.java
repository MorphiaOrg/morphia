package dev.morphia.critter.sources;

import org.bson.codecs.pojo.PropertyAccessor;

public class DummyEntityNameAccessorModel implements PropertyAccessor<String> {
    @Override
    public <S> String get(S entity) {
        return null;// ((DummyEntity) entity).__readName();
    }

    @Override
    public <S> void set(S entity, String value) {
//        ((DummyEntity) entity).__writeName(value);
    }
}
