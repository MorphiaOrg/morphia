package com.google.code.morphia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.code.morphia.utils.IndexDirection;

/**
 * Specified on fields that should be Indexed.
 * 
 * @author Scott Hernandez
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Indexed {
	/** Indicates the direction of the index (ascending, descending, or both */
	IndexDirection value() default IndexDirection.ASC;
	/** The name of the index to create */
	String name() default "";
	/** Creates the index as unique value index; inserting duplicates will cause errors */
	boolean unique() default false;
	/** Tells the unique index to drop duplicates silently when creating; only the first will be kept*/
	boolean dropDups() default false;
}
