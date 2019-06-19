package dev.morphia.mapping;


import dev.morphia.Datastore;
import dev.morphia.mapping.cache.EntityCache;
import org.bson.Document;

import java.util.Map;


/**
 * @morphia.internal
 * @deprecated
 */
public interface CustomMapper {
    /**
     * Creates an entity and populates its state based on the document given.  This method is primarily an internal method.  Reliance on
     * this method may break your application in future releases.
     * @param datastore the Datastore to use
     * @param document  the object state to use
     * @param mf        the MappedField with the metadata to use during conversion
     * @param entity    the entity to populate
     * @param cache     the EntityCache to use to prevent multiple loads of the same entities over and over
     * @param mapper    the Mapper to use
     */
    void fromDocument(final Datastore datastore, Document document, MappedField mf, Object entity, EntityCache cache, Mapper mapper);

    /**
     * Converts an entity to a Document.  This method is primarily an internal method. Reliance on this method may break your application
     * in
     * future releases.
     *
     * @param entity          the entity to convert
     * @param mf              the MappedField with the metadata to use during conversion
     * @param document        the Document to populate
     * @param involvedObjects a Map of objects already seen
     * @param mapper          the Mapper to use
     */
    void toDocument(Object entity, MappedField mf, Document document, Map<Object, Document> involvedObjects, Mapper mapper);
}
