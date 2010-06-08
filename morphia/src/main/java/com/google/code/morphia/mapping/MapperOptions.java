package com.google.code.morphia.mapping;

import java.lang.annotation.Annotation;

import com.google.code.morphia.annotations.Embedded;

/**
 * Options to control mapping behavior.
 * 
 * @author Scott Hernandez
 */
public class MapperOptions {
	/** <p>Treat java transient fields as if they have <code>@Transient</code> on them</p> */
	public boolean actLikeSerializer = false;
	public Class<? extends Annotation> defaultFieldAnnotation = Embedded.class;
	/** <p>Controls if null are stored. </p>*/
	public boolean storeNulls = false;
	/** <p>Controls if empty collection/arrays are stored. </p>*/
	public boolean storeEmpties = false;
	/** <p>Controls if final fields are stored. </p>*/
	public boolean ignoreFinals = false; //ignore final fields.
}
