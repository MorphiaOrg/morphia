package com.google.code.morphia.annotations;

public @interface Createable {
    /** Specify the concrete class to instantiate. */
    Class<?> concreteClass() default Object.class;
	
}
