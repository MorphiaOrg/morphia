package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.MorphiaModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.sofia.Sofia;
import org.bson.codecs.pojo.InstanceCreatorFactory;

import java.lang.reflect.Constructor;

/**
 * @param <T>
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class InstanceCreatorFactoryImpl<T> implements InstanceCreatorFactory<T> {
    private Constructor<T> noArgsConstructor;
    private MorphiaModel model;

    /**
     * Creates a factory for this type
     *
     * @param model the type's model
     */
    public InstanceCreatorFactoryImpl(final MorphiaModel model) {
        this.model = model;
    }

    @Override
    public MorphiaInstanceCreator<T> create() {
        if (!model.getType().isInterface()) {
            if (ConstructorCreator.getFullConstructor(model) != null) {
                return new ConstructorCreator(model);
            }

            for (Constructor<?> constructor : model.getType().getDeclaredConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    noArgsConstructor = (Constructor<T>) constructor;
                    noArgsConstructor.setAccessible(true);
                    return new NoArgCreator<>(noArgsConstructor);
                }
            }
        }
        throw new MappingException(Sofia.cannotInstantiate(model.getType().getName(), null));
    }
}
