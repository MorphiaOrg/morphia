package dev.morphia.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Properties for capped collections; used in {@link Entity}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CappedAt {
    /**
     * @return count of items to cap at (defaults to unlimited)
     */
    long count() default 0;

    /**
     * @return size to cap at (defaults to 1MB)
     */
    long value() default 1024 * 1024;
}
