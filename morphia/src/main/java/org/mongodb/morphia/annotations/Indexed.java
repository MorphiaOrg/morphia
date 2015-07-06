package org.mongodb.morphia.annotations;


import org.mongodb.morphia.utils.IndexDirection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specified on fields that should be Indexed.
 *
 * @author Scott Hernandez
 */
@SuppressWarnings("deprecation")
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Indexed {
    /**
     * Create the index in the background?
     */
    boolean background() default false;

    /**
     * Tells the unique index to drop duplicates silently when creating; only the first will be kept
     */
    boolean dropDups() default false;

    /**
     * defines the time to live for documents in the collection
     */
    int expireAfterSeconds() default -1;

    /**
     * The name of the index to create; default is to let the mongodb create a name (in the form of key1_1/-1_key2_1/-1...)
     */
    String name() default "";

    /**
     * Create the index with the sparse option
     */
    boolean sparse() default false;

    /**
     * Creates the index as a unique value index; inserting duplicates values in this field will cause errors
     */
    boolean unique() default false;

    /**
     * Indicates the direction of the index (ascending, descending, or both; default is ascending
     */
    IndexDirection value() default IndexDirection.ASC;

}
