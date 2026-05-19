package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Morphia entity indicating it should be mapped.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Entity {
    /**
     * @return The capped collection configuration options
     */
    CappedAt cap() default @CappedAt(value = -1, count = -1);

    /**
     * @return The default write concern to use when dealing with this entity
     */
    String concern() default "";

    /**
     * @return the discriminator value to use for this type.
     */
    String discriminator() default ".";

    /**
     * @return the discriminator key to use for this type.
     */
    String discriminatorKey() default ".";

    /**
     * @return true if the discriminator for this type should be stored
     */
    boolean useDiscriminator() default true;

    /**
     * @return the collection name to for this entity. Defaults to the class's simple name
     * @see Class#getSimpleName()
     */
    String value() default ".";
}
