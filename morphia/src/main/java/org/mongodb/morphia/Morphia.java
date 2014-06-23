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


package org.mongodb.morphia;


import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;


/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
public class Morphia {
    private final Mapper mapper;
    private boolean useBulkWriteOperations = false;

    public Morphia() {
        this(new Mapper(), Collections.<Class>emptySet());
    }

    public Morphia(final Mapper mapper) {
        this(mapper, Collections.<Class>emptySet());
    }

    public Morphia(final Set<Class> classesToMap) {
        this(new Mapper(), classesToMap);
    }

    public Morphia(final Mapper mapper, final Set<Class> classesToMap) {
        this.mapper = mapper;
        for (final Class c : classesToMap) {
            map(c);
        }
    }

    public synchronized Morphia map(final Class... entityClasses) {
        if (entityClasses != null && entityClasses.length > 0) {
            for (final Class entityClass : entityClasses) {
                if (!mapper.isMapped(entityClass)) {
                    mapper.addMappedClass(entityClass);
                }
            }
        }
        return this;
    }

    public synchronized Morphia map(final Set<Class> entityClasses) {
        if (entityClasses != null && !entityClasses.isEmpty()) {
            for (final Class entityClass : entityClasses) {
                if (!mapper.isMapped(entityClass)) {
                    mapper.addMappedClass(entityClass);
                }
            }
        }
        return this;
    }

    public Morphia mapPackageFromClass(final Class clazz) {
        return mapPackage(clazz.getPackage().getName(), false);
    }

    /**
     * Tries to map all classes in the package specified. Fails if one of the classes is not valid for mapping.
     *
     * @param packageName the name of the package to process
     * @return the Morphia instance
     */
    public synchronized Morphia mapPackage(final String packageName) {
        return mapPackage(packageName, false);
    }

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName          the name of the package to process
     * @param ignoreInvalidClasses specifies whether to ignore classes in the package that cannot be mapped
     * @return the Morphia instance
     */
    public synchronized Morphia mapPackage(final String packageName, final boolean ignoreInvalidClasses) {
        try {
            for (final Class clazz : ReflectionUtils.getClasses(packageName)) {
                try {
                    final Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(clazz);
                    final Entity entityAnn = ReflectionUtils.getClassEntityAnnotation(clazz);
                    final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
                    if ((entityAnn != null || embeddedAnn != null) && !isAbstract) {
                        map(clazz);
                    }
                } catch (final MappingException ex) {
                    if (!ignoreInvalidClasses) {
                        throw ex;
                    }
                }
            }
            return this;
        } catch (IOException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        } catch (ClassNotFoundException e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    /**
     * Check whether a specific class is mapped by this instance.
     *
     * @param entityClass the class we want to check
     * @return true if the class is mapped, else false
     */
    public boolean isMapped(final Class entityClass) {
        return mapper.isMapped(entityClass);
    }

    public <T> T fromDBObject(final Class<T> entityClass, final DBObject dbObject) {
        return fromDBObject(entityClass, dbObject, mapper.createEntityCache());
    }

    public <T> T fromDBObject(final Class<T> entityClass, final DBObject dbObject, final EntityCache cache) {
        if (!entityClass.isInterface() && !mapper.isMapped(entityClass)) {
            throw new MappingException("Trying to map to an unmapped class: " + entityClass.getName());
        }
        try {
            return (T) mapper.fromDBObject(entityClass, dbObject, cache);
        } catch (Exception e) {
            throw new MappingException("Could not map entity from DBObject", e);
        }
    }

    public DBObject toDBObject(final Object entity) {
        try {
            return mapper.toDBObject(entity);
        } catch (Exception e) {
            throw new MappingException("Could not map entity to DBObject", e);
        }
    }

    public Mapper getMapper() {
        return mapper;
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param mongoClient the representations of the connection to a MongoDB instance
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public Datastore createDatastore(final MongoClient mongoClient, final String dbName) {
        return new DatastoreImpl(this, mongoClient, dbName);
    }

    /**
     * Creates a new Datastore for interacting with MongoDB using POJOs
     *
     * @param mongoClient the representations of the connection to a MongoDB instance
     * @param mapper      a pre-configured Mapper for your POJOs
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    public Datastore createDatastore(final MongoClient mongoClient, final Mapper mapper, final String dbName) {
        return new DatastoreImpl(this, mapper, mongoClient, dbName);
    }

    public boolean getUseBulkWriteOperations() {
        return useBulkWriteOperations;
    }

    public boolean isUseBulkWriteOperations() {
        return useBulkWriteOperations;
    }

    public void setUseBulkWriteOperations(final boolean useBulkWriteOperations) {
        this.useBulkWriteOperations = useBulkWriteOperations;
    }
}
