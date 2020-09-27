package dev.morphia.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>Annotation which helps migrate schemas by loading one of several possible properties in the document into fields or methods.  This is
 * typically used when a field is renamed, allowing the field to be populated by both its current name and any prior names.</p>
 * <p/>
 * When placed on a field, the additional names (document field) will be checked when this field is loaded.  If the document contains data
 * for more than one of the names, an exception will be thrown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface AlsoLoad {
    /**
     * @return An array of alternative fields to load should the primary field name be missing in a document.
     */
    String[] value();
}
