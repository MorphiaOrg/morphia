package dev.morphia.mapping;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.internal.ConstructorCreator;
import dev.morphia.sofia.Sofia;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @morphia.internal
 */
@MorphiaInternal
public class InstanceCreatorFactoryImpl implements InstanceCreatorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceCreatorFactoryImpl.class);

    private final EntityModel model;
    private Function<Conversions, MorphiaInstanceCreator> creator;

    /**
     * Creates a factory for this type
     *
     * @param model the type's model
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public InstanceCreatorFactoryImpl(EntityModel model) {
        this.model = model;
    }

    @Override
    public MorphiaInstanceCreator create(Conversions conversions) {
        if (creator == null) {
            if (!model.getType().isInterface()) {
                Constructor<?> constructor = ConstructorCreator.bestConstructor(model);
                if (constructor != null) {
                    creator = (c) -> new ConstructorCreator(model, constructor, c);
                } else {
                    LOG.debug("using old creator approach: " + model.getType().getName());
                    try {
                        Constructor<?> declared = model.getType().getDeclaredConstructor();
                        creator = (c) -> new NoArgCreator(declared);
                    } catch (NoSuchMethodException e) {
                        Constructor<?> full = ConstructorCreator.getFullConstructor(model);
                        creator = (c) -> new ConstructorCreator(model, full, c);
                    }
                }
            }

            if (creator == null) {
                throw new MappingException(Sofia.noSuitableConstructor(model.getType().getName()));
            }
        }

        return creator.apply(conversions);
    }
}
