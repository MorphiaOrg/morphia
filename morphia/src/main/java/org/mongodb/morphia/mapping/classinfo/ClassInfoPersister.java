package org.mongodb.morphia.mapping.classinfo;

import com.mongodb.DBObject;

/**
 * A strategy interface for persisting class information within {@link DBObject} so that they can be mapped to POJOs.
 */
public interface ClassInfoPersister extends Cacheable {

    /**
     * Add the information necessary to rehydrate the concrete class of this object
     * @param entity the pojo
     * @param dbObject the data to be serialized
     * @param hint any e.g. super class of the type
     */
    void addClassInfo(Object entity, DBObject dbObject, Class<?> hint);

    /**
     * Load a class from class information stored within the object; return null if none found.
     *
     * @param <T> the desired type for convenience
     * @param dbObject the data from which to load the class information
     * @param hint any e.g. super class of the returned type if already known; not null
     * @return the class object or null
     */
    <T> Class<T> getClass(DBObject dbObject, Class<?> hint);

     /**
     * Load a class from class information stored within the object; return null if none found.
     *
     * Likely to be called when mapping e.g. elemn of
     *
     * @param <T> the desired type for convenience
     * @param dbObject the data from which to load the class information
     * @return the class object or null
     */
    <T> Class<T> getClass(DBObject dbObject);

    /**
     * Remove any known class information stored within the data. If no info is present, this is a no-op.
     *
     * @param dbObject the data
     * @param hint the type (or superclass thereof) expected to be represented by the data
     */
    void removeClassInfo(DBObject dbObject, Class<?> hint);

    /**
     * Remove any class information stored within the data. If no info is present, this is a no-op.
     *
     * @param dbObject the data
     */
    void removeClassInfo(DBObject dbObject);

    /**
     * Add class information to the provided projection
     *
     * @param projection the projection
     * @param hint the class expected
     */
    void addClassInfoToProjection(DBObject projection, Class<?> hint);

}
