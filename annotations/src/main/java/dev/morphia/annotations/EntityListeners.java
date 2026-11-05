package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.morphia.EntityListener;

/**
 * Specifies other classes to participate in the @Entity's lifecycle. Each class implements one or more of the per-event listener
 * interfaces, such as {@code PrePersistListener}, and has a no-argument constructor. Morphia creates an instance of each when it
 * maps the entity. A listed class may not declare methods annotated with a lifecycle event annotation: only an entity's own
 * methods can be.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface EntityListeners {
    /**
     * @return The listeners to use for this entity
     */
    Class<? extends EntityListener<?>>[] value() default {};
}
