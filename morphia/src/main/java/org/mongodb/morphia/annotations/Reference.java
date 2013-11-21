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


package org.mongodb.morphia.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mongodb.morphia.mapping.Mapper;


/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {
  /**
   * The name of the Mongo value to store the field. Defaults to the name of the field being annotated.
   *
   * @return the name of the Mongo value storing the field value
   */
  String value() default Mapper.IGNORED_FIELDNAME;

  /**
   * Specify the concrete class to instantiate.
   */
  Class<?> concreteClass() default Object.class;

  /**
   * Ignore any reference that don't resolve (aren't in mongodb)
   */
  boolean ignoreMissing() default false;

  /**
   * Create a proxy around the reference which will be resolved on the first method call.
   */
  boolean lazy() default false;

  /**
   * Specifies whether only _id should be stored versus storing a DBRef
   */
  boolean idOnly() default false;
}
