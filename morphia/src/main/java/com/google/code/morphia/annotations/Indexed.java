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
	IndexDirection value() default IndexDirection.ASC;
}
