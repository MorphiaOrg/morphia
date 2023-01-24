package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Called before the data has been persisted to the datastore (before mapping is done).
 * Here you can alter your class members prior to being saved.
 * <p>
 * Methods with this annotation may optionally take a parameter of type
 * {@link dev.morphia.Datastore Datastore}. This is the Datastore that
 * data will be persisted to.
 * <p>
 * <strong>Example declarations (in order of most to least typical):</strong>
 * 
 * <pre>
 * {@code @PrePersist}
 * void cleanUpBeforeSaving() {
 *   // perform variable sanitization here as needed
 * }
 * </pre>
 * 
 * <pre>
 * {@code @PrePersist}
 * void retrieveOtherInfoBeforeSaving(Datastore datastore) {
 *   // query datastore as needed
 * }
 * </pre>
 * 
 * @see dev.morphia.annotations.PreLoad
 * @see dev.morphia.annotations.PostLoad
 * @see dev.morphia.annotations.PostPersist
 *
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface PrePersist {
}
