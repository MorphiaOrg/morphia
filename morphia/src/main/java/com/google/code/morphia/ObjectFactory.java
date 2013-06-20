package com.google.code.morphia;


import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
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
