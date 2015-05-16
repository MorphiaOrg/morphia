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


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Properties for capped collections; used in {@link Entity}
 *
 * @author Scott Hernandez
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CappedAt {
  /**
   * size to cap at (defaults to 1MB)
   */
  long value() default 1024 * 1024;

  /**
   * count of items to cap at (defaults to unlimited)
   */
  long count() default 0;
}
