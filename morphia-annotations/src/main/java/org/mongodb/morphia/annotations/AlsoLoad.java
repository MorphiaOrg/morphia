package org.mongodb.morphia.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>Annotation which helps migrate schemas by loading one of several possible properties in the document into fields or methods.  This is
 * typically used when a field is renamed, allowing the field to be populated by both its current name and any prior names.</p>
 * <p/>
 * When placed on a field, the additional names (document field) will be checked when this field is loaded.  If the document
 * contains data for more than one of the names, an exception will be thrown. 
 * (orig @author Jeff Schnitzer <jeff@infohazard.org> for Objectify)
 *
 * @author Scott Hernandez
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD})
public @interface AlsoLoad {
  String[] value();
}