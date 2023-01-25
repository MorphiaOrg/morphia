package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Called after the data has been loaded into the java object. This is a good place to perform
 * initialization and sanity-checking of the object.
 * <p>
 * Methods with this annotation may optionally take a parameter of type {@link org.bson.Document Document}
 * and/or a parameter of type {@link dev.morphia.Datastore Datastore}. If both parameters are used,
 * the order is unimportant.
 * <p>
 * The Document parameter (if used) will be the document that this object was loaded from. The Datastore
 * parameter (if used) will be the datastore this object was loaded from.
 * <p>
 * <strong>Example declarations (in order of most to least typical):</strong>
 * 
 * <pre>
 * {@code @PostLoad}
 * void cleanUpAfterLoading() {
 *   // perform initialization here as needed.
 * }
 * </pre>
 * 
 * <pre>
 * {@code @PostLoad}
 * void examineAdditionalDocumentElements(Document doc) {
 *   // doc is the Document we were loaded from. Document elements can be examined manually here. 
 *   // Note that changes to doc will not have any effect (as it's already been read from).
 * }
 * </pre>
 * 
 * <pre>
 * {@code @PostLoad}
 * void performComplexAdditionalQueriesAfterLoading(Document doc, Datastore datastore) {
 *   // doc is the Document we were loaded from. datastore is the datastore we were loaded from.
 * }
 * </pre>
 * 
 * @see dev.morphia.annotations.PreLoad
 * @see dev.morphia.annotations.PrePersist
 * @see dev.morphia.annotations.PostPersist
 *
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface PostLoad {
}
