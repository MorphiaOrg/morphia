package org.mongodb.morphia;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import com.mongodb.DBObject;


@SuppressWarnings("rawtypes")
public interface ObjectFactory {
  Object createInstance(Class clazz);

  Object createInstance(Class clazz, DBObject dbObj);

  Object createInstance(Mapper mapper, MappedField mf, DBObject dbObj);

  Map createMap(MappedField mf);

  List createList(MappedField mf);

  Set createSet(MappedField mf);
}
