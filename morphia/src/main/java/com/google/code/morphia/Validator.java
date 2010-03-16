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
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoEmbedded;
import com.google.code.morphia.annotations.MongoID;
import com.google.code.morphia.annotations.MongoReference;
import com.google.code.morphia.annotations.MongoValue;
import com.google.code.morphia.utils.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Validator {

    private static final Logger logger = Logger.getLogger(Validator.class.getName());

    Set<Class> validate(Class c) {
        Set<Class> validClasses = new HashSet<Class>();
        validateInternal(c, validClasses);
        return validClasses;
    }

    private void validateInternal(Class c, Set<Class> validClasses ) {
        if (!validClasses.contains(c)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("Processing class: " + c.getName());
            }

            // we ignore interfaces
            if ( !c.isInterface() ) {
                validateFields(c, ReflectionUtils.getDeclaredAndInheritedFields(c, true));
                validClasses.add(c);
            }
        }
    }

    private void validateFields(Class c, Field[] fields ) {
        MongoEmbedded classMongoEmbedded = ReflectionUtils.getClassMongoEmbeddedAnnotation(c);
        MongoDocument classMongoDocument = ReflectionUtils.getClassMongoDocumentAnnotation(c);

        if ( classMongoDocument == null && classMongoEmbedded == null ) {
            throw new MongoMappingException(
                    "In [" + c.getName()
                           + "]: This class is neither annotated as @MongoDocument or @MongoEmbedded.");
        }
        if ( classMongoDocument != null && classMongoEmbedded != null ) {
            throw new MongoMappingException(
                    "In [" + c.getName()
                           + "]: Cannot have both @MongoDocument and @MongoEmbedded annotation at class level.");
        }

        boolean foundIDField = false;
        boolean foundCollectionNameField = false;

        for (Field field : fields) {
            field.setAccessible(true);
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("In [" + c.getName() + "]: Processing field: " + field.getName());
            }

            if ( field.isAnnotationPresent(MongoValue.class) ) {
                // make sure that the property type is supported
                if ( !ReflectionUtils.implementsInterface(field.getType(), List.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Set.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Map.class)
                        && !ReflectionUtils.isPropertyType(field.getType())
                        ) {
                    throw new MongoMappingException("In [" + c.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @MongoValue is of type that cannot be mapped (type is "
                            + field.getType().getName() + ").");
                }

            } else if (field.isAnnotationPresent(MongoID.class)) {
            	foundIDField = true;
            	if (field.getAnnotation(MongoID.class).useObjectId()) {
	                // make sure this is a String field
	                if (field.getType() != String.class) {
	                    throw new MongoMappingException("In [" + c.getName() + "]: Field [" + field.getName()
	                            + "] which is annotated as @MongoID must be of type java.lang.String, but is of type: "
	                            + field.getType().getName());
	                }
            	}

            } else if (field.isAnnotationPresent(MongoCollectionName.class)) {
                // make sure this is a String field
                if (field.getType() != String.class) {
                    throw new MongoMappingException("In [" + c.getName() + "]: Field [" + field.getName()
                            + "] which is annotated as @MongoCollectionName must be of type java.lang.String, but is of type: "
                            + field.getType().getName());
                }
                foundCollectionNameField = true;

            } else if (field.isAnnotationPresent(MongoEmbedded.class)) {
                if ( !ReflectionUtils.implementsInterface(field.getType(), List.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Set.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Map.class)
                        && (!field.getType().isInterface() && ReflectionUtils.getClassMongoEmbeddedAnnotation(field.getType()) == null) ) {

                    throw new MongoMappingException(
                            "In ["
                                    + c.getName()
                                    + "]: Field ["
                                    + field.getName()
                                    + "] which is annotated as @MongoEmbedded is of type [" + field.getType().getName() + "] which cannot be embedded.");
                }

            } else if (field.isAnnotationPresent(MongoReference.class)) {
                if ( !ReflectionUtils.implementsInterface(field.getType(), List.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Set.class)
                        && !ReflectionUtils.implementsInterface(field.getType(), Map.class)
                        && (!field.getType().isInterface() && ReflectionUtils.getClassMongoDocumentAnnotation(field.getType()) == null) ) {

                    throw new MongoMappingException(
                            "In ["
                                    + c.getName()
                                    + "]: Field ["
                                    + field.getName()
                                    + "] which is annotated as @MongoReference is of type [" + field.getType().getName() + "] which cannot be referenced.");
                }
            }
        }



        if (!foundIDField && classMongoDocument != null) {
            throw new MongoMappingException("In [" + c.getName() + "]: No field is annotated with @MongoID");
        }
        if (!foundCollectionNameField && classMongoDocument != null) {
            throw new MongoMappingException("In [" + c.getName() + "]: No field is annotated with @MongoCollectionName");
        }
    }
}
