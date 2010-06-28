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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 *
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 **/
@SuppressWarnings("unchecked")
public class Morphia {
	private final Mapper mapper;

	public Morphia() {
        this(Collections.EMPTY_SET);
    }

    public Morphia( Set<Class> classesToMap ) {
        this.mapper = new Mapper();
        for (Class c : classesToMap) {
            map(c);
        }
    }

    public synchronized Morphia map(Class entityClass) {
        if ( !mapper.isMapped(entityClass) ) {
            mapper.addMappedClass(entityClass);
        }
        return this;
    }

    public synchronized Morphia mapPackageFromClass(Class clazz) {
        return mapPackage(clazz.getPackage().getName(), false);
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
                    Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(c);
                    Entity enityAnn = ReflectionUtils.getClassEntityAnnotation(c);
                    if ( enityAnn != null || embeddedAnn != null ) {
                        map(c);
                    }
                } catch (MappingException ex) {
                    if (!ignoreInvalidClasses) {
                        throw ex;
                    }
                }
            }
            return this;
        } catch (IOException ioex) {
            throw new MappingException("Could not get map classes from package " + packageName, ioex);
        } catch (ClassNotFoundException cnfex) {
            throw new MappingException("Could not get map classes from package " + packageName, cnfex);
        }
    }

    /**
     * Get a set of all classes that are mapped by this instance.
     *
     * @return all classes that are mapped by this instance
     */
    public Map<String, MappedClass> getMappedClasses() {
        return Collections.unmodifiableMap(mapper.getMappedClasses());
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

    public <T> T fromDBObject(Class<T> entityClass, DBObject dbObject) {
        if ( !entityClass.isInterface() && !mapper.isMapped(entityClass)) {
            throw new MappingException("Trying to map to an unmapped class: " + entityClass.getName());
        }
        try {
            return (T) mapper.fromDBObject(entityClass, (BasicDBObject) dbObject);
        } catch ( Exception e ) {
            throw new MappingException("Could not map entity from DBObject", e); }
    }

    public DBObject toDBObject( Object entity ) {
        try {
            return mapper.toDBObject(entity);
        } catch ( Exception e ) {
            throw new MappingException("Could not map entity to DBObject", e); }
    }

    public Mapper getMapper() { return this.mapper; }

    public Datastore createDatastore(String dbName) { 
    	return createDatastore(dbName, null, null);
	}
    
    public Datastore createDatastore(String dbName, String user, char[] pw) {
    	try {
			return createDatastore(new Mongo(), dbName, user, pw);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
   
    public Datastore createDatastore(Mongo mon) {
    	return new DatastoreImpl(this, mon);
    }

    public Datastore createDatastore(Mongo mon, String dbName, String user, char[] pw) {
    	return new DatastoreImpl(this, mon, dbName, user, pw);
    }
}
