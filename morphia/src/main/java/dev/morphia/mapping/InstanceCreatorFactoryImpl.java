package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import org.bson.codecs.pojo.InstanceCreatorFactory;

import java.lang.reflect.Constructor;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class InstanceCreatorFactoryImpl<T> implements InstanceCreatorFactory<T> {
    private Constructor<T> noArgsConstructor;

    /**
     * Creates a factory for this type
     *
     * @param type the type to create
     */
    public InstanceCreatorFactoryImpl(final Class type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                noArgsConstructor = (Constructor<T>) constructor;
                noArgsConstructor.setAccessible(true);
            }
        }
        if (!type.isInterface() && noArgsConstructor == null) {
            throw new MappingException("Can not find a no arg constructor for " + type);
        }
    }

    @Override
    public MorphiaInstanceCreator<T> create() {
        return new NoArgCreator<>(noArgsConstructor);
    }
}
