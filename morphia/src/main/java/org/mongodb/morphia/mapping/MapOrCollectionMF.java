package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressWarnings({"rawtypes"})
public class MapOrCollectionMF extends MappedField {
    private ParameterizedType pType;
    private Object value;

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

    @Override
    public String getNameToStore() {
        return "superFake";
    }

    @Override
    public Object getDbObjectValue(final DBObject dbObj) {
        return dbObj;
    }

    @Override
    public boolean hasAnnotation(final Class ann) {
        return Embedded.class.equals(ann);
    }

    @Override
    public String toString() {
        return "MapOrCollectionMF for " + super.toString();
    }

    @Override
    public Class getType() {
        return isMap ? Map.class : List.class;
    }

    @Override
    public Class getMapKeyClass() {
        return (Class) (isMap() ? pType.getActualTypeArguments()[0] : null);
    }

    @Override
    public Type getSubType() {
        return pType.getActualTypeArguments()[isMap() ? 1 : 0];
    }

    @Override
    public Class getSubClass() {
        return toClass(getSubType());
    }

    @Override
    public boolean isSingleValue() {
        return false;
    }

    @Override
    public Object getFieldValue(final Object classInst) {
        return value;
    }

    @Override
    public void setFieldValue(final Object classInst, final Object val) {
        value = val;
    }
}