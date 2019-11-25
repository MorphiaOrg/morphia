package dev.morphia.mapping;


import com.mongodb.DBObject;
import dev.morphia.Datastore;
import dev.morphia.mapping.cache.EntityCache;

import java.util.Map;


/**
 * @morphia.internal
 * @deprecated
 */
public interface CustomMapper {
    /**
     * Creates an entity and populates its state based on the dbObject given.  This method is primarily an internal method.  Reliance on
     * this method may break your application in future releases.
     * @param datastore the Datastore to use
     * @param dbObject  the object state to use
     * @param mf        the MappedField with the metadata to use during conversion
     * @param entity    the entity to populate
     * @param cache     the EntityCache to use to prevent multiple loads of the same entities over and over
     * @param mapper    the Mapper to use
     */
    void fromDBObject(Datastore datastore, DBObject dbObject, MappedField mf, Object entity, EntityCache cache, Mapper mapper);

    /**
     * Converts an entity to a DBObject.  This method is primarily an internal method. Reliance on this method may break your application
     * in
     * future releases.
     *
     * @param entity          the entity to convert
     * @param mf              the MappedField with the metadata to use during conversion
     * @param dbObject        the DBObject to populate
     * @param involvedObjects a Map of objects already seen
     * @param mapper          the Mapper to use
     */
    void toDBObject(Object entity, MappedField mf, DBObject dbObject, Map<Object, DBObject> involvedObjects, Mapper mapper);
}
