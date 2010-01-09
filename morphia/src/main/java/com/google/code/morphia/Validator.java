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

package com.google.code.morphia;

import com.google.code.morphia.annotations.MongoCollectionName;
import com.google.code.morphia.annotations.MongoEmbedded;
import com.google.code.morphia.annotations.MongoID;
import com.google.code.morphia.annotations.MongoReference;
import com.google.code.morphia.annotations.MongoValue;
import com.google.code.morphia.utils.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Validator {

    private static final Logger logger = Logger.getLogger(Validator.class.getName());

    Set<Class> validate(Class c, boolean dynamicInstantiation) {
        Set<Class> validClasses = new HashSet<Class>();
        validateInternal(c, validClasses, dynamicInstantiation, true, false);
        return validClasses;
    }

    private void validateInternal(Class c, Set<Class> validClasses, boolean dynamicInstantiation,
            boolean idFieldNeeded, boolean collectionNameFieldNeeded ) {
        if (!validClasses.contains(c)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("Processing class: " + c.getName());
            }

            validClasses.add(c);

            // when dynamic instantiation is turned on, we ignore interfaces
            if (!(c.isInterface() && dynamicInstantiation)) {
                validateFields(c, ReflectionUtils.getDeclaredAndInheritedFields(c, true), validClasses,
                        dynamicInstantiation, idFieldNeeded, collectionNameFieldNeeded);
            }
        }
    }

    private void validateFields(Class c, Field[] fields, Set<Class> validClasses, boolean dynamicInstantiation,
            boolean idFieldNeeded, boolean collectionNameFieldNeeded ) {
        boolean foundIDField = false;
        boolean foundCollectionNameField = false;
        for (Field field : fields) {
            field.setAccessible(true);
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("In [" + c.getName() + "]: Processing field: " + field.getName());
            }

            if ( field.isAnnotationPresent(MongoValue.class) ) {
                // make sure that the property type is supported
                if (ReflectionUtils.implementsInterface(field.getType(), List.class)) {
                    if (!ReflectionUtils.isFieldParameterizedWithPropertyType(field)) {
                        throw new MongoMappingException(
                                "In ["
                                        + c.getName()
                                        + "]: Field ["
                                        + field.getName()
                                        + "] which is a List annotated as @MongoValue is not parameterized with an invalid type.");
                    }
                } else if (!ReflectionUtils.isPropertyType(field.getType())) {
                    throw new MongoMappingException("In [" + c.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @MongoValue is of type that cannot be mapped (type is "
                            + field.getType().getName() + ").");
                }

            } else if (field.isAnnotationPresent(MongoID.class)) {
                // make sure this is a String field
                if (field.getType() != String.class) {
                    throw new MongoMappingException("In [" + c.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @MongoID must be of type java.lang.String, but is of type: "
                            + field.getType().getName());
                }
                foundIDField = true;

            } else if (field.isAnnotationPresent(MongoCollectionName.class)) {
                // make sure this is a String field
                if (field.getType() != String.class) {
                    throw new MongoMappingException("In [" + c.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @MongoCollectionName must be of type java.lang.String, but is of type: "
                            + field.getType().getName());
                }
                foundCollectionNameField = true;

            } else if (field.isAnnotationPresent(MongoEmbedded.class)) {
                validateInternal(field.getType(), validClasses, dynamicInstantiation, false, false);

            } else if (field.isAnnotationPresent(MongoReference.class)) {
                Class fieldType;
                if (ReflectionUtils.implementsInterface(field.getType(), List.class)) {
                    fieldType = ReflectionUtils.getParameterizedClass(field);
                } else {
                    fieldType = field.getType();
                }

                if (fieldType != null) {
                    // validate the class
                    validateInternal(fieldType, validClasses, dynamicInstantiation, true, true);
                }
            }
        }
        if (!foundIDField && idFieldNeeded) {
            throw new MongoMappingException("In [" + c.getName() + "]: No field is annotated with @MongoID");
        }
        if (!foundCollectionNameField && collectionNameFieldNeeded) {
            throw new MongoMappingException("In [" + c.getName() + "]: No field is annotated with @MongoCollectionName");
        }
    }
}
