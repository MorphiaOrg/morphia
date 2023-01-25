package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Called before the data has been loaded into the object. Here you can alter the raw BSON Document from the
 * datastore prior to object initialization.
 * <p>
 * Methods with this annotation may optionally take a parameter of type {@link org.bson.Document Document}
 * and/or a parameter of type {@link dev.morphia.Datastore Datastore}. If both parameters are used, the
 * order is unimportant. For typical usage, the method should take a Document parameter.
 * <p>
 * The Document parameter (if used) will be the document that this object will be loaded from. You can alter
 * the Document here and those changes will be reflected when the object is loaded. The Datastore parameter
 * (if used) will be the Datastore this Document was loaded from.
 * <p>
 * <strong>Example declarations (in order of most to least typical):</strong>
 * 
 * <pre>
 * {@code @PreLoad}
 * private void fixupDocument(Document doc) {
 *   // doc is the Document we will be initialized from. Document elements can be manually changed or removed.
 * }
 * </pre>
 * 
 * <pre>
 * {@code @PreLoad}
 * private void performAdditionalQueriesBeforeLoading(Document doc, Datastore datastore) {
 *   // doc is the Document we will be loaded from. datastore is the datastore the Document originated from.
 * }
 * </pre>
 * 
 * @see dev.morphia.annotations.PostLoad
 * @see dev.morphia.annotations.PrePersist
 * @see dev.morphia.annotations.PostPersist
 *
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface PreLoad {
}
