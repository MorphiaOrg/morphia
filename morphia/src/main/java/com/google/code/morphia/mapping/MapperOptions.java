package com.google.code.morphia.mapping;

import java.lang.annotation.Annotation;

import com.google.code.morphia.annotations.Embedded;

/**
 * Options to control mapping behavior.
 * 
 * @author Scott Hernandez
 */
public class MapperOptions {
	public boolean actLikeSerializer = false;
	public Class<? extends Annotation> defaultFieldAnnotation = Embedded.class;
	public boolean storeNulls = false;
	public boolean storeEmpties = false;
	public boolean ignoreFinals = false;
	
}
