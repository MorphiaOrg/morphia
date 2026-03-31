package dev.morphia.critter.sources;

import org.bson.codecs.pojo.PropertyAccessor;

public class ExampleSalaryAccessorTemplate implements PropertyAccessor<Long> {
    @Override
    public <S> Long get(S entity) {
        return ((Example) entity).__readSalaryTemplate();
    }

    public <S> void set(S entity, Long value) {
        ((Example) entity).__writeSalaryTemplate(value);
    }
}
