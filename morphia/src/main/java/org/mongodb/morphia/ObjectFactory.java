package org.mongodb.morphia;

import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ObjectFactory is used by morphia to create instances of classes which can be customized to fit a particular applications needs.
 */
public interface ObjectFactory {
    /**
     * Creates an instance of the given class.
     */
    <T> T createInstance(Class<T> clazz);

    /**
     * Creates an instance of the class defined in the {@link Mapper#CLASS_NAME_FIELDNAME} field in the dbObject passed in.  If that field
     * is missing, the given Class is used instead.
     */
    <T> T createInstance(Class<T> clazz, DBObject dbObj);

    /**
     * Creates an instance of the class defined in the {@link Mapper#CLASS_NAME_FIELDNAME} field in the dbObject passed in.  If that field
     * is missing, morphia attempts to the MappedField to determine which concrete class to instantiate.
     */
    Object createInstance(Mapper mapper, MappedField mf, DBObject dbObj);

    /**
     * Defines how morphia creates a Map object. 
     */
    Map createMap(MappedField mf);

    /**
     * Defines how morphia creates a List object. 
     */
    List createList(MappedField mf);

    /**
     * Defines how morphia creates a Set object. 
     */
    Set createSet(MappedField mf);
}
