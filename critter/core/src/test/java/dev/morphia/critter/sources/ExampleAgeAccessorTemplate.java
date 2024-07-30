package dev.morphia.critter.sources;

import org.bson.codecs.pojo.PropertyAccessor;

public class ExampleAgeAccessorTemplate implements PropertyAccessor<Integer> {
    @Override
    public <S> Integer get(S entity) {
        return ((Example) entity).__readAge();
    }

    public <S> void set(S entity, Integer value) {
        ((Example) entity).__writeAge(value);
    }
}
