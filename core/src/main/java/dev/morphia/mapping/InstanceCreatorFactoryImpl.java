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
    private MorphiaInstanceCreator creator;

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
        if (creator == null) {
            if (!model.getType().isInterface()) {
                try {
                    creator = new NoArgCreator(model.getType().getDeclaredConstructor());
                } catch (NoSuchMethodException e) {
                    creator = new ConstructorCreator(model, ConstructorCreator.getFullConstructor(model));
                }
            } else {
                throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
            }
        }

        return creator;
    }
}
