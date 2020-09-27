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


package dev.morphia.annotations;

import dev.morphia.mapping.Mapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows marking and naming the collectionName
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Entity {
    /**
     * @return The capped collection configuration options
     */
    CappedAt cap() default @CappedAt(value = -1, count = -1);

    /**
     * @return The default write concern to use when dealing with this entity
     */
    String concern() default "";

    /**
     * @return true if the discriminator for this type should be stored
     */
    boolean useDiscriminator() default true;

    /**
     * @return the collection name to for this entity.  Defaults to the class's simple name
     * @see Class#getSimpleName()
     */
    String value() default Mapper.IGNORED_FIELDNAME;

    /**
     * @return the discriminator key to use for this type.
     */
    String discriminatorKey() default Mapper.IGNORED_FIELDNAME;

    /**
     * @return the discriminator value to use for this type.
     */
    String discriminator() default Mapper.IGNORED_FIELDNAME;
}

