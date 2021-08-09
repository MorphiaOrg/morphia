package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.sofia.Sofia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * @morphia.internal
 */
public class InstanceCreatorFactoryImpl implements InstanceCreatorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceCreatorFactoryImpl.class);

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
                Constructor<?> constructor = ConstructorCreator.bestConstructor(model);
                if (constructor != null) {
                    creator = () -> new ConstructorCreator(model, constructor);
                } else {
                    LOG.info("using old creator approach: " + model.getType().getName());
                    try {
                        Constructor<?> declared = model.getType().getDeclaredConstructor();
                        creator = () -> new NoArgCreator(declared);
                    } catch (NoSuchMethodException e) {
                        try {
                            Constructor<?> full = ConstructorCreator.getFullConstructor(model);
                            creator = () -> new ConstructorCreator(model, full);
                        } catch (MappingException ignored) {
                            //                            MorphiaInstanceCreator unsafeConstructorCreator = new UnsafeConstructorCreator
                            //                            (model);
                            //                            creator = () -> unsafeConstructorCreator;
                        }
                    }
                }
            }

            if (creator == null) {
                throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
            }
        }

        return creator.get();
    }
}
