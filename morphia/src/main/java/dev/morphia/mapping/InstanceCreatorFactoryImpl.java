package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.sofia.Sofia;

/**
 * @param <T>
 * @morphia.internal
 */
public class InstanceCreatorFactoryImpl<T> implements InstanceCreatorFactory<T> {
    private EntityModel<T> model;

    /**
     * Creates a factory for this type
     *
     * @param model the type's model
     */
    public InstanceCreatorFactoryImpl(final EntityModel<T> model) {
        this.model = model;
    }

    public MorphiaInstanceCreator<T> create() {
        if (!model.getType().isInterface()) {
            if (ConstructorCreator.getFullConstructor(model) != null) {
                return new ConstructorCreator<>(model);
            }

            try {
                return new NoArgCreator<>(model.getType().getDeclaredConstructor());
            } catch (NoSuchMethodException e) {
                throw new MappingException(Sofia.noargConstructorNotFound(model.getType().getName()));

            }
        }
        throw new MappingException(Sofia.noargConstructorNotFound(model.getType().getName()));
    }
}
