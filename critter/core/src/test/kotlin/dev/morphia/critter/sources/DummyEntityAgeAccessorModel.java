package dev.morphia.critter.sources;

import org.bson.codecs.pojo.PropertyAccessor;

public class DummyEntityAgeAccessorModel implements PropertyAccessor<Integer> {
    @Override
    public <S> Integer get(S entity) {
        return ((DummyEntity) entity).__readAgeSample();
    }

    public <S> void set(S entity, Integer value) {
        ((DummyEntity) entity).__writeAgeSample(value);
    }
}
