package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.Constructor;

/**
 * @morphia.internal
 */
public class InstanceCreatorFactoryImpl implements InstanceCreatorFactory {
    private final EntityModel model;
    private Constructor<?> noArgConstructor;
    private Constructor<?> fullConstructor;

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
            try {
                if (noArgConstructor == null) {
                    noArgConstructor = model.getType().getDeclaredConstructor();
                }
                return new NoArgCreator(noArgConstructor);
            } catch (NoSuchMethodException e) {
                if (fullConstructor == null) {
                    fullConstructor = ConstructorCreator.getFullConstructor(model);
                }
                if (fullConstructor != null) {
                    return new ConstructorCreator(model, fullConstructor);
                }
            }
        }
        throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
    }
}
