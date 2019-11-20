package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
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
    private EntityModel model;

    /**
     * Creates a factory for this type
     *
     * @param model the type's model
     */
    public InstanceCreatorFactoryImpl(final EntityModel model) {
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
                    return new NoArgCreator<>((Constructor<T>) constructor);
                }
            }
        }
        throw new MappingException(Sofia.cannotInstantiate(model.getType().getName(), null));
    }
}
