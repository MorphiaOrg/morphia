package org.mongodb.morphia.mapping;

import com.mongodb.DBObject;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a MappedField facade that allows us to convert and collect values to be gathered back in to a Map or Collection, e.g., rather 
 * than directly on a mapped entity.  This are not mapped directly to a field on a class like MappedFields are.
 */
public class EphemeralMappedField extends MappedField {
    private ParameterizedType pType;
    private Object value;
    private MappedField parent;

    public EphemeralMappedField(final ParameterizedType t, final MappedField mf, final Mapper mapper) {
        super(t, mapper);
        parent = mf;
        pType = t;
        final Class rawClass = (Class) t.getRawType();
        setIsSet(ReflectionUtils.implementsInterface(rawClass, Set.class));
        setIsMap(ReflectionUtils.implementsInterface(rawClass, Map.class));
        setMapKeyType(getMapKeyClass());
        setSubType(getSubType());
        setIsMongoType(ReflectionUtils.isPropertyType(getSubClass()));
    }

    public EphemeralMappedField(final Type t, final MappedField mf, final Mapper mapper) {
        super(t, mapper);
        parent = mf;
    }

    public MappedField getParent() {
        return parent;
    }

    @Override
    public void addAnnotation(final Class<? extends Annotation> clazz) {
    }

    @Override
    public void addAnnotation(final Class<? extends Annotation> clazz, final Annotation ann) {
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
        return "EphemeralMappedField for " + super.toString();
    }

    @Override
    public Class getType() {
        if (pType == null) {
            return super.getType();
        } else if (isMap()) {
            return Map.class;
        } else {
            return List.class;
        }
    }

    @Override
    public Class getMapKeyClass() {
        return (Class) (isMap() ? pType.getActualTypeArguments()[0] : null);
    }

    @Override
    public Type getSubType() {
        return pType != null ? pType.getActualTypeArguments()[isMap() ? 1 : 0] : null;
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

    @Override
    protected String getMappedFieldName() {
        return "";
    }
}