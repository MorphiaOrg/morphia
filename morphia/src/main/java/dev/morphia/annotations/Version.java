package dev.morphia.annotations;


import dev.morphia.mapping.Mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * supposed to be used on a Long or long field for optimistic locking.
 *
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Version {
    /**
     * @return the field name to use in the document.  Defaults to the java field name.
     */
    String value() default Mapper.IGNORED_FIELDNAME;
}
