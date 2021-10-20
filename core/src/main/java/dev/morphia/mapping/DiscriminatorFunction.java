package dev.morphia.mapping;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;

/**
 * Defines a function to calculate a discriminator value.  This function is only applied if the existing value is the annotation default
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
            public String compute(EntityModelBuilder builder) {
                return builder.type().getName();
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
            public String compute(EntityModelBuilder builder) {
                return builder.type().getName().toLowerCase();
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
            public String compute(EntityModelBuilder builder) {
                return builder.type().getSimpleName().toLowerCase();
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
            public String compute(EntityModelBuilder builder) {
                return builder.type().getSimpleName();
            }
        };
    }

    /**
     * Applies the function to the given model to determine the discriminator value
     *
     * @param builder the builder to evaluate
     */
    public final void apply(EntityModelBuilder builder) {
        String discriminator = Mapper.IGNORED_FIELDNAME;
        Entity entity = builder.getAnnotation(Entity.class);
        if (entity != null) {
            discriminator = entity.discriminator();
        } else {
            Embedded embedded = builder.getAnnotation(Embedded.class);
            if (embedded != null) {
                discriminator = embedded.discriminator();
            }
        }
        if (discriminator.equals(Mapper.IGNORED_FIELDNAME)) {
            discriminator = compute(builder);
        }

        builder.discriminator(discriminator);
    }

    protected abstract String compute(EntityModelBuilder builder);
}
