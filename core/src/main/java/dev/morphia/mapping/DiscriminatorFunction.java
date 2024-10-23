package dev.morphia.mapping;

import dev.morphia.mapping.discriminator.ClassNameDiscriminator;
import dev.morphia.mapping.discriminator.LowerClassNameDiscriminator;
import dev.morphia.mapping.discriminator.LowerSimpleNameDiscriminator;
import dev.morphia.mapping.discriminator.SimpleNameDiscriminator;

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
        return new ClassNameDiscriminator();
    }

    /**
     * Defines a function to use the lowercase class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction lowerClassName() {
        return new LowerClassNameDiscriminator();
    }

    /**
     * Defines a function to use the lowercase simple class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction lowerSimpleName() {
        return new LowerSimpleNameDiscriminator();
    }

    /**
     * Defines a function to use the simple class name for the discriminator value
     *
     * @return the function
     */
    public static DiscriminatorFunction simpleName() {
        return new SimpleNameDiscriminator();
    }

    /**
     * Applies the function to the given model to determine the discriminator value
     *
     * @param type
     * @param discriminator
     * @return
     * @hidden
     */
    public final String apply(Class<?> type, String discriminator) {
        return discriminator.equals(Mapper.IGNORED_FIELDNAME) ? compute(type) : discriminator;
    }

    /**
     * Computes the discriminator value for an Entity
     * 
     * @return the discriminator value
     * @param type
     */
    public abstract String compute(Class<?> type);

}
