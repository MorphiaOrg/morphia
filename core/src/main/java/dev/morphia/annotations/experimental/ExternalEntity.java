package dev.morphia.annotations.experimental;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.mapping.Mapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a "stand in" for an external class whose source can not be properly annotated.  A class annotated with this
 * annotation will be processed like any class annotated with {@link Entity} however, the result information stored will be reference the
 * target type instead of the annotated type.  In this way third party classes can mapped properly in spite of not having access to the
 * source, e.g.  See the documentation for more detail.
 *
 * @morphia.experimental
 * @since 2.3
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@MorphiaExperimental
public @interface ExternalEntity {
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
    String discriminator() default Mapper.IGNORED_FIELDNAME;

    /**
     * @return the discriminator key to use for this type.
     */
    String discriminatorKey() default Mapper.IGNORED_FIELDNAME;

    /**
     * The external target type being mapped.
     *
     * @return the external target type
     */
    Class<?> target();

    /**
     * @return true if the discriminator for this type should be stored
     */
    boolean useDiscriminator() default true;

    /**
     * @return the collection name to for this entity.  Defaults to the class's simple name
     * @see Class#getSimpleName()
     */
    String value() default Mapper.IGNORED_FIELDNAME;
}
