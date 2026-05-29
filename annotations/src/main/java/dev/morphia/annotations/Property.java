package dev.morphia.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation for specifying persistence behavior
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Property {

    /**
     * @return the concrete class to instantiate.
     */
    Class<?> concreteClass() default Object.class;

    /**
     * @return the field name to use in the document. Defaults to the java field name.
     */
    String value() default ".";
}
