package dev.morphia.annotations;

import dev.morphia.mapping.codec.PropertyCodec;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a specific handler for a type above and beyond the codecs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Documented
@Inherited
public @interface Handler {
    /**
     * @return the handler Class
     */
    Class<? extends PropertyCodec> value();
}
