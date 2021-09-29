package dev.morphia.mapping.experimental;

import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.InvocationTargetException;

public class ReflectionConstructorCreator implements MorphiaInstanceCreator {

    private final Object instance;

    public ReflectionConstructorCreator(EntityModel model) {
        try {
            instance = ConstructorlessInstance.create(model.getType());
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
        }
    }

    @Override
    public Object getInstance() {
        return instance;
    }

    @Override
    public void set(Object value, PropertyModel model) {
        model.getAccessor().set(instance, value);
    }
}
