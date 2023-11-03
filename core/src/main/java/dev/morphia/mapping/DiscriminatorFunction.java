package dev.morphia.mapping;

import dev.morphia.annotations.Entity;
import dev.morphia.mapping.codec.pojo.EntityModel;

/**
 * Defines a function to calculate a discriminator value. This function is only applied if the existing value is the annotation default
 * value.
 */
@SuppressWarnings("deprecation")
public abstract class DiscriminatorFunction {
    /**
     * Defines a function to use the class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction className() {
        return new DiscriminatorFunction() {
            @Override
            public String compute(EntityModel model) {
                return model.getType().getName();
            }
        };
    }

    /**
     * Defines a function to use the lowercase class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction lowerClassName() {
        return new DiscriminatorFunction() {
            @Override
            public String compute(EntityModel model) {
                return model.getType().getName().toLowerCase();
            }
        };
    }

    /**
     * Defines a function to use the lowercase simple class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction lowerSimpleName() {
        return new DiscriminatorFunction() {
            @Override
            public String compute(EntityModel model) {
                return model.getType().getSimpleName().toLowerCase();
            }
        };
    }

    /**
     * Defines a function to use the simple class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction simpleName() {
        return new DiscriminatorFunction() {
            @Override
            public String compute(EntityModel model) {
                return model.getType().getSimpleName();
            }
        };
    }

    /**
     * Applies the function to the given model to determine the discriminator value
     *
     * @param model the model to evaluate
     * @hidden
     */
    public final void apply(EntityModel model) {
        String discriminator = Mapper.IGNORED_FIELDNAME;
        Entity entity = model.getAnnotation(Entity.class);
        if (entity != null) {
            discriminator = entity.discriminator();
        }
        if (discriminator.equals(Mapper.IGNORED_FIELDNAME)) {
            discriminator = compute(model);
        }

        model.discriminator(discriminator);
    }

    /**
     * Computes the discriminator value for an Entity
     * 
     * @param model the model
     * @return the discriminator value
     */
    protected abstract String compute(EntityModel model);
}
