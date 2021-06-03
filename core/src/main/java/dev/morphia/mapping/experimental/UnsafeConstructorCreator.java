package dev.morphia.mapping.experimental;

import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.sofia.Sofia;

public class UnsafeConstructorCreator implements MorphiaInstanceCreator {
    private static final UnsafeAllocator UNSAFE_ALLOCATOR = UnsafeAllocator.create();
    private final Object instance;

    public UnsafeConstructorCreator(EntityModel model) {
        try {
            instance = UNSAFE_ALLOCATOR.newInstance(model.getType());
        } catch (Exception e) {
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
