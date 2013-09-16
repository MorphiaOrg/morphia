package org.mongodb.morphia.mapping;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.utils.ReflectionUtils;
import com.mongodb.DBObject;


@SuppressWarnings({ "rawtypes" })
public class MapOrCollectionMF extends MappedField {
  private ParameterizedType pType;
  private Object            value;

  /* (non-Javadoc) @see java.lang.Object#clone() */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    final MapOrCollectionMF other = new MapOrCollectionMF();
    other.pType = pType;
    other.isSet = isSet;
    other.isMap = isMap;
    other.mapKeyType = mapKeyType;
    other.subType = subType;
    other.isMongoType = isMongoType;
    return other;
  }

  MapOrCollectionMF() {
    isSingleValue = false;
  }

  MapOrCollectionMF(final ParameterizedType t) {
    this();
    pType = t;
    final Class rawClass = (Class) t.getRawType();
    isSet = ReflectionUtils.implementsInterface(rawClass, Set.class);
    isMap = ReflectionUtils.implementsInterface(rawClass, Map.class);
    mapKeyType = getMapKeyClass();
    subType = getSubType();
    isMongoType = ReflectionUtils.isPropertyType(getSubClass());
  }

  public Object getValue() {
    return value;
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getNameToStore()
   */
  @Override
  public String getNameToStore() {
    return "superFake";
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getDbObjectValue(com.mongodb.DBObject)
   */
  @Override
  public Object getDbObjectValue(final DBObject dbObj) {
    return dbObj;
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#hasAnnotation(java.lang.Class)
   */
  @Override
  public boolean hasAnnotation(final Class ann) {
    return Embedded.class.equals(ann);
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#toString()
   */
  @Override
  public String toString() {
    return "MapOrCollectionMF for " + super.toString();
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getType()
   */
  @Override
  public Class getType() {
    return isMap ? Map.class : List.class;
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getMapKeyClass()
   */
  @Override
  public Class getMapKeyClass() {
    return (Class) (isMap() ? pType.getActualTypeArguments()[0] : null);
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getSubType()
   */
  @Override
  public Type getSubType() {
    return pType.getActualTypeArguments()[isMap() ? 1 : 0];
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getSubClass()
   */
  @Override
  public Class getSubClass() {
    return toClass(getSubType());
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#isSingleValue()
   */
  @Override
  public boolean isSingleValue() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#getFieldValue(java.lang.Object)
   */
  @Override
  public Object getFieldValue(final Object classInst) throws IllegalArgumentException {
    return value;
  }

  /* (non-Javadoc)
   * @see org.mongodb.morphia.mapping.MappedField#setFieldValue(java.lang.Object, java.lang.Object)
   */
  @Override
  public void setFieldValue(final Object classInst, final Object val) throws IllegalArgumentException {
    value = val;
  }


}
