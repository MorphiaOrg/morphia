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

            try {
                return new NoArgCreator<>((Constructor<T>) model.getType().getDeclaredConstructor(new Class[0]));
            } catch (NoSuchMethodException e) {
                throw new MappingException(Sofia.noargConstructorNotFound(model.getType().getName()));

            }
        }
        throw new MappingException(Sofia.noargConstructorNotFound(model.getType().getName()));
    }
}
