package dev.morphia.mapping;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.Constructor;

/**
 * @morphia.internal
 */
@MorphiaInternal
public class NoArgCreator implements MorphiaInstanceCreator {
    private Object instance;
    private final Constructor<?> noArgsConstructor;

    /**
     * Creates the creator
     *
     * @param noArgsConstructor the constructor
     */
    public NoArgCreator(Constructor<?> noArgsConstructor) {
        this.noArgsConstructor = noArgsConstructor;
        this.noArgsConstructor.setAccessible(true);
    }

    @Override
    public void set(Object value, PropertyModel model) {
        model.getAccessor().set(instance(), value);
    }

    private Object instance() {
        if (instance == null) {
            try {
                instance = noArgsConstructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new MappingException(Sofia.instantiationProblem(noArgsConstructor.getDeclaringClass().getName(), e.getMessage()), e);
            }
        }
        return instance;
    }

    @Override
    public Object getInstance() {
        return instance();
    }
}
