/*
 *  Copyright 2010 gauti.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */


package org.mongodb.morphia.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Defines an index
 *
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Index {
    /**
     * List of fields (prepended with "-" for desc; defaults to asc).  If a value is defined for {@link #fields()} this value will be 
     * ignored and logged.
     * 
     * @deprecated use {@link #fields()}
     */
    String value() default "";

    /**
     * List of fields to include in the index.  At least one field must be defined unless defining a {@link Indexes#text() text index}.  
     * Use of this field implies use of {@link #options()} and any options defined directly on this annotation will be ignored.
     */
    Field[] fields() default {};
    
    
    /**
     * Options to apply to the index.  Use of this field will ignore any of the deprecated options defined on {@link Index} directly.
     */
    IndexOptions options() default @IndexOptions(userDefined = false);

    /**
     * The name of the index to create; default is to let the mongodb create a name (in the form of key1_1/-1_key2_1/-1...)
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    String name() default "";

    /**
     * Creates the index as a unique value index; inserting duplicates values in this field will cause errors
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    boolean unique() default false;

    /**
     * Tells the unique index to drop duplicates silently when creating; only the first will be kept
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    boolean dropDups() default false;

    /**
     * Create the index in the background
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    boolean background() default false;

    /**
     * Create the index with the sparse option
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    boolean sparse() default false;

    /**
     * disables validation for the field name
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    boolean disableValidation() default false;

    /**
     * defines the time to live for documents in the collection
     * @deprecated use the {@link IndexOptions} found in {@link #options()}
     */
    int expireAfterSeconds() default -1;

}
