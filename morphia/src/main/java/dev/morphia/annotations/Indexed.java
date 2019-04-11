package dev.morphia.annotations;


import dev.morphia.utils.IndexDirection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specified on fields that should be Indexed.
 *
 * @author Scott Hernandez
 */
@SuppressWarnings("deprecation")
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Indexed {
    /**
     * @return Options to apply to the index. Use of this field will ignore any of the deprecated options defined on {@link Index} directly.
     */
    IndexOptions options() default @IndexOptions();

    /**
     * @return if true, creates the index in the background if true
     *
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    @Deprecated
    boolean background() default false;

    /**
     * @return if true, tells the unique index to drop duplicates silently when creating; only the first will be kept
     *
     * @deprecated Support for this has been removed from the server.  This value is ignored.
     */
    @Deprecated
    boolean dropDups() default false;

    /**
     * @return the time to live for documents in the collection
     *
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    @Deprecated
    int expireAfterSeconds() default -1;

    /**
     * @return The name of the index to create; default is to let the mongodb create a name (in the form of key1_1/-1_key2_1/-1...)
     *
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    @Deprecated
    String name() default "";

    /**
     * @return if true, create the index with the sparse option
     *
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    @Deprecated
    boolean sparse() default false;

    /**
     * @return if true, creates the index as a unique value index; inserting duplicates values in this field will cause errors
     *
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    @Deprecated
    boolean unique() default false;

    /**
     * @return the type of the index (ascending, descending, geo2d); default is ascending
     * @see IndexDirection
     */
    IndexDirection value() default IndexDirection.ASC;
}
