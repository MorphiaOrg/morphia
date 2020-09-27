/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dev.morphia.annotations;


import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.mongodb.client.model.ValidationAction.ERROR;
import static com.mongodb.client.model.ValidationLevel.STRICT;


/**
 * Defines the document validation logic for a collection.
 *
 * @since 1.3
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Validation {
    /**
     * @return the query used to validate documents in a collection.  This query is not validated so care must be taken to ensure
     * document field names and types are correct.
     *
     * @mongodb.driver.manual core/document-validation/
     */
    String value();

    /**
     * @return how strictly MongoDB should apply the validation rules to existing documents during an insert or update.
     * @see ValidationLevel
     */
    ValidationLevel level() default STRICT;

    /**
     * @return how strictly MongoDB should apply the validation rules to existing documents during an insert or update.
     * @see ValidationAction
     */
    ValidationAction action() default ERROR;

}
