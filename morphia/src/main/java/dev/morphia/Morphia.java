/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */


package dev.morphia;


import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;


/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
public class Morphia {
    private static final Logger LOG = LoggerFactory.getLogger(Morphia.class);
    private final Mapper mapper;

    /**
     * Creates a Morphia instance with a default Mapper and an empty class set.
     */
    public Morphia() {
        this(new Mapper(), Collections.<Class>emptySet());
    }

    /**
     * Creates a Morphia instance with the given Mapper and class set.
     *
     * @param mapper       the Mapper to use
     * @param classesToMap the classes to map
     */
    public Morphia(final Mapper mapper, final Set<Class> classesToMap) {
        this.mapper = mapper;
        for (final Class c : classesToMap) {
            map(c);
        }
    }

    /**
     * Creates a Morphia instance with the given Mapper
     *
     * @param mapper the Mapper to use
     */
    public Morphia(final Mapper mapper) {
        this(mapper, Collections.<Class>emptySet());
    }

    /**
     * Creates a Morphia instance with the given classes
     *
     * @param classesToMap the classes to map
     */
    public Morphia(final Set<Class> classesToMap) {
        this(new Mapper(), classesToMap);
    }

    /**
     * It is best to use a Mongo singleton instance here.
     *
     * @param mongoClient the representations of the connection to a MongoDB instance
     * @param dbName      the name of the database
     * @return a Datastore that you can use to interact with MongoDB
     */
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    public Datastore createDatastore(final MongoClient mongoClient, final Mapper mapper, final String dbName) {
        return new DatastoreImpl(this, mapper, mongoClient, dbName);
    }

    /**
     * Creates an entity and populates its state based on the dbObject given.  This method is primarily an internal method.  Reliance on
     * this method may break your application in future releases.
     *
     * @param <T>         type of the entity
     * @param datastore   the Datastore to use when fetching this reference
     * @param entityClass type to create
     * @param dbObject    the object state to use
     * @return the newly created and populated entity
     */
    public <T> T fromDBObject(final Datastore datastore, final Class<T> entityClass, final DBObject dbObject) {
        return fromDBObject(datastore, entityClass, dbObject, mapper.createEntityCache());
    }

    /**
     * Creates an entity and populates its state based on the dbObject given.  This method is primarily an internal method.  Reliance on
     * this method may break your application in future releases.
     *
     * @param <T>         type of the entity
     * @param datastore   the Datastore to use when fetching this reference
     * @param entityClass type to create
     * @param dbObject    the object state to use
     * @param cache       the EntityCache to use to prevent multiple loads of the same entities over and over
     * @return the newly created and populated entity
     */
    public <T> T fromDBObject(final Datastore datastore, final Class<T> entityClass, final DBObject dbObject, final EntityCache cache) {
        if (!entityClass.isInterface() && !mapper.isMapped(entityClass)) {
            throw new MappingException("Trying to map to an unmapped class: " + entityClass.getName());
        }
        try {
            return mapper.fromDBObject(datastore, entityClass, dbObject, cache);
        } catch (Exception e) {
            throw new MappingException("Could not map entity from DBObject", e);
        }
    }

    /**
     * @return the mapper used by this instance of Morphia
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * @return false.  Setting this value has no value functionally or performance-wise.
     * @deprecated
     * @see <a href="https://github.com/MorphiaOrg/morphia/issues/1052">Issue #1052</a>
     */
    @Deprecated
    public boolean getUseBulkWriteOperations() {
        return false;
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

    /**
     * @return false.  Setting this value has no value functionally or performance-wise.
     * @deprecated
     * @see <a href="https://github.com/MorphiaOrg/morphia/issues/1052">Issue #1052</a>
     */
    @Deprecated
    public boolean isUseBulkWriteOperations() {
        return false;
    }

    /**
     * Configures Morphia to use bulk writes.  Only useful with MongoDB 2.6+.
     *
     * @param useBulkWriteOperations true if Morphia should use bulk writes
     * @see <a href="https://github.com/MorphiaOrg/morphia/issues/1052">Issue #1052</a>
     * @deprecated Setting this value has no value functionally or performance-wise.
     */
    @Deprecated
    public void setUseBulkWriteOperations(final boolean useBulkWriteOperations) {
    }

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return this
     */
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

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return this
     */
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
            for (final Class clazz : ReflectionUtils.getClasses(mapper.getOptions().getClassLoader(), packageName,
                mapper.getOptions().isMapSubPackages())) {
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
        } catch (Exception e) {
            throw new MappingException("Could not get map classes from package " + packageName, e);
        }
    }

    /**
     * Maps all the classes found in the package to which the given class belongs.
     *
     * @param clazz the class to use when trying to find others to map
     * @return this
     */
    public Morphia mapPackageFromClass(final Class clazz) {
        return mapPackage(clazz.getPackage().getName(), false);
    }

    /**
     * Converts an entity to a DBObject.  This method is primarily an internal method. Reliance on this method may break your application
     * in
     * future releases.
     *
     * @param entity the entity to convert
     * @return the DBObject
     */
    public DBObject toDBObject(final Object entity) {
        try {
            return mapper.toDBObject(entity);
        } catch (Exception e) {
            throw new MappingException("Could not map entity to DBObject", e);
        }
    }
}
