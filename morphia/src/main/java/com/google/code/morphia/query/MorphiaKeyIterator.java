package com.google.code.morphia.query;


import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.Mapper;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


/**
 * @author Scott Hernandez
 */
public class MorphiaKeyIterator<T> extends MorphiaIterator<T, Key<T>> {
  public MorphiaKeyIterator(final DBCursor cursor, final Mapper m, final Class<T> clazz, final String kind) {
    super(cursor, m, clazz, kind, null);
  }

  @Override
  protected Key<T> convertItem(final DBObject dbObj) {
    final Key<T> key = new Key<T>(kind, dbObj.get(Mapper.ID_KEY));
    key.setKindClass(clazz);
    return key;
  }

}