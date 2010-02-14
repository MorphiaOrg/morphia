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

import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class Morphia {

    private final Mapper mapper;
    private final Validator validator;

    public Morphia() {
        this(Collections.EMPTY_SET);
    }

    public Morphia( Set<Class> classesToMap ) {
        this.mapper = new Mapper();
        this.validator = new Validator();
        for (Class c : classesToMap) {
            map(c);
        }
    }

    public synchronized Morphia map(Class entityClass) {
        if ( !mapper.isMapped(entityClass) ) {
            Set<Class> validClasses = validator.validate(entityClass);
            for (Class c : validClasses) {
                mapper.addMappedClass(c);
            }
        }
        return this;
    }

    /**
     * Tries to map all classes in the package specified. Fails if one of the classes is not valid for mapping.
     *
     * @param packageName
     *            the name of the package to process
     * @return the Morphia instance
     */
    public synchronized Morphia mapPackage(String packageName) {
        return mapPackage(packageName, false);
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName
     *            the name of the package to process
     * @param ignoreInvalidClasses
     *            specifies whether to ignore classes in the package that cannot be mapped
     * @return the Morphia instance
     */
    public synchronized Morphia mapPackage(String packageName, boolean ignoreInvalidClasses) {
        try {
            for (Class c : ReflectionUtils.getClasses(packageName)) {
                try {
                    map(c);
                } catch (MongoMappingException ex) {
                    if (!ignoreInvalidClasses) {
                        throw ex;
                    }
                }
            }
            return this;
        } catch (IOException ioex) {
            throw new MongoMappingException("Could not get map classes from package " + packageName, ioex);
        } catch (ClassNotFoundException cnfex) {
            throw new MongoMappingException("Could not get map classes from package " + packageName, cnfex);
        }
    }

    /**
     * Get a set of all classes that are mapped by this instance.
     *
     * @return all classes that are mapped by this instance
     */
    public Set<Class> getMappedClasses() {
        return Collections.unmodifiableSet(mapper.getMappedClasses());
    }

    /**
     * Check whether a specific class is mapped by this instance.
     *
     * @param entityClass
     *            the class we want to check
     * @return true if the class is mapped, else false
     */
    public boolean isMapped(Class entityClass) {
        return mapper.isMapped(entityClass);
    }

    public <T> T fromDBObject(Class<T> entityClass, BasicDBObject dbObject) {
        if ( !entityClass.isInterface() && !mapper.isMapped(entityClass)) {
            throw new MongoMappingException("Trying to map to an unmapped class: " + entityClass.getName());
        }
        try {
            return (T) mapper.fromDBObject(entityClass, (BasicDBObject) dbObject);
        } catch ( Exception e ) {
            throw new MongoMappingException("Could not map from DBObject", e);
        } finally {
            mapper.clearHistory();
        }
    }

    public DBObject toDBObject( Object entity ) {
        if (!mapper.isMapped(entity.getClass())) {
            throw new MongoMappingException("Trying to map an unmapped class: " + entity.getClass().getName());
        }
        try {
            return mapper.toDBObject(entity);
        } catch ( Exception e ) {
            throw new MongoMappingException("Could not map to DBObject", e);
        } finally {
            mapper.clearHistory();
        }
    }
}
