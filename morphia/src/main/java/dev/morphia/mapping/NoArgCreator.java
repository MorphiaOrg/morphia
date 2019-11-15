package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.sofia.Sofia;
import org.bson.codecs.pojo.PropertyModel;

import java.lang.reflect.Constructor;

/**
 * @param <E>
 * @morphia.internal
 */
public class NoArgCreator<E> implements MorphiaInstanceCreator<E> {
    private E instance;
    private Constructor<E> noArgsConstructor;

    /**
     * Creates the creator
     *
     * @param noArgsConstructor the constructor
     */
    public NoArgCreator(final Constructor<E> noArgsConstructor) {
        this.noArgsConstructor = noArgsConstructor;
        this.noArgsConstructor.setAccessible(true);
    }

    @Override
    public <S> void set(final S value, final PropertyModel<S> propertyModel) {
        propertyModel.getPropertyAccessor().set(instance(), value);
    }

    private E instance() {
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
    public E getInstance() {
        return instance();
    }
}
