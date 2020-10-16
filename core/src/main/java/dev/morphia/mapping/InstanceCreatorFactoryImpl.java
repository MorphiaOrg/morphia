package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.sofia.Sofia;

/**
 * @morphia.internal
 */
public class InstanceCreatorFactoryImpl implements InstanceCreatorFactory {
    private final EntityModel model;

    /**
     * Creates a factory for this type
     *
     * @param model the type's model
     */
    public InstanceCreatorFactoryImpl(EntityModel model) {
        this.model = model;
    }

    @Override
    public MorphiaInstanceCreator create() {
        if (!model.getType().isInterface()) {
            if (ConstructorCreator.getFullConstructor(model) != null) {
                return new ConstructorCreator(model);
            }

            try {
                return new NoArgCreator(model.getType().getDeclaredConstructor());
            } catch (NoSuchMethodException e) {
                throw new MappingException(Sofia.noargConstructorNotFound(model.getType().getName()));

            }
        }
        throw new MappingException(Sofia.noargConstructorNotFound(model.getType().getName()));
    }
}
