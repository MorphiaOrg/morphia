package org.mongodb.morphia;


import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressWarnings("rawtypes")
public interface ObjectFactory {
    <T> T createInstance(Class<T> clazz);

    <T> T createInstance(Class<T> clazz, DBObject dbObj);

    Object createInstance(Mapper mapper, MappedField mf, DBObject dbObj);

    Map createMap(MappedField mf);

    List createList(MappedField mf);

    Set createSet(MappedField mf);
}
