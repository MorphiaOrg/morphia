package dev.morphia.mapping;

import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ReflectionConstructorCreator;
import dev.morphia.mapping.experimental.UnsafeConstructorCreator;
import dev.morphia.sofia.Sofia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
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
                //try to find zero args constructor first
                Constructor<?> noArgsConstructor = Arrays.stream(model.getType().getDeclaredConstructors())
                        .filter(it -> it.getParameters().length == 0)
                        .findFirst()
                        .orElse(null);

                if (noArgsConstructor != null) {
                    creator = () -> new NoArgCreator(noArgsConstructor);
                } else { //try instantiating using reflection
                    try {
                        creator = () -> new ReflectionConstructorCreator(model);
                    } catch (MappingException ex) {
                        //as last resort, use unsafe constructor allocation
                        creator = () -> new UnsafeConstructorCreator(model);
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
