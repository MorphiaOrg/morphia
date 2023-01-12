package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Called after the data has been persisted from the java object. Here you can alter the 
 * BSON Document before it is saved.
 * <p>
 * Methods with this annotation may optionally take a parameter of type {@link org.bson.Document Document} 
 * and/or a parameter of type {@link dev.morphia.Datastore Datastore}. If both parameters are used, the
 * order is unimportant. For typical usage, the method should take a Document parameter.
 * <p>
 * The Document parameter (if used) will be the document that this object was saved to. You can alter 
 * the Document to change how it will be saved. The Datastore parameter (if used) will be the
 * Datastore this Document will be saved to.
 * <p>
 * <strong>Example declarations (in order of most to least typical):</strong>
 * <pre>
 * {@code @PostPersist}
 * private void removeUnneededDataFromDoc(Document doc) {
 *   // doc is the Document this object was written to. It can be altered here prior to saving.
 * }
 * </pre>
 * <pre>
 * {@code @PostPersist}
 * private void performAdditionalQueriesBeforeSaving(Document doc, Datastore datastore) {
 *   // doc is the Document this object was written to. datastore is where it will be saved in.
 *   // doc can be altered here prior to saving.
 * }
 * </pre>
 *
 * @see dev.morphia.annotations.PreLoad
 * @see dev.morphia.annotations.PostLoad
 * @see dev.morphia.annotations.PrePersist
 *
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface PostPersist {
}
