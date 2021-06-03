package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.mapping.experimental.UnsafeConstructorCreator;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * @morphia.internal
 */
public class InstanceCreatorFactoryImpl implements InstanceCreatorFactory {
    private final EntityModel model;
    private Supplier<MorphiaInstanceCreator> creator;

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
                    Constructor<?> constructor = model.getType().getDeclaredConstructor();
                    creator = () -> new NoArgCreator(constructor);
                } catch (NoSuchMethodException e) {
                    try {
                        Constructor<?> constructor = ConstructorCreator.getFullConstructor(model);
                        creator = () -> {
                            return new ConstructorCreator(model, constructor);
                        };
                    } catch (MappingException e1) {
                        MorphiaInstanceCreator unsafeConstructorCreator = new UnsafeConstructorCreator(model);
                        creator = () -> unsafeConstructorCreator;
                    }
                }
            } else {
                throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
            }
        }

        return creator.get();
    }
}
