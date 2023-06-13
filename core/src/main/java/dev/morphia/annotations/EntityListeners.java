package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.morphia.EntityListener;

/**
 * Specifies other classes to participate in the @Entity's lifecycle
 *
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface EntityListeners {
    /**
     * @return The listeners to use for this entity
     * @deprecated In the next version, this will be restricted to subclasses of {@link EntityListener}. Migrating your listeners to be
     *             subclasses now will prevent any compilation issues in the future.
     */
    @Deprecated(since = "2.4.0")
    Class<?>[] value() default {};
}
