/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface MongoEmbedded {

    /**
     * The name of the Mongo value to store the field.
     * Defaults to the name of the field being annotated.
     *
     * @return the name of the Mongo value storing the field value
     */
    String value() default "fieldName";

    /**
     * Specify the implementing class to user for List.
     *
     * @return The implementing class of the list
     */
    Class<? extends List> listClass() default ArrayList.class;

    /**
     * Specify the implementing class to user for Set.
     *
     * @return The implementing class of the set
     */
    Class<? extends Set> setClass() default HashSet.class;

    /**
     * Specify the implementing class to user for Map.
     *
     * @return The implementing class of the map
     */
    Class<? extends Map> mapClass() default HashMap.class;
}
