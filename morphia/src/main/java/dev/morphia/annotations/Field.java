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


import dev.morphia.utils.IndexType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a field to be used in an index;
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Field {
    /**
     * @return "Direction" of the indexing.  Defaults to {@link IndexType#ASC}.
     *
     * @see IndexType
     */
    IndexType type() default IndexType.ASC;

    /**
     * @return Field name to index
     */
    String value();

    /**
     * @return The weight to use when creating a text index.  This value only makes sense when direction is {@link IndexType#TEXT}
     */
    int weight() default -1;
}
