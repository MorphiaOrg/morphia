package dev.morphia.mapping.codec.pojo.critter;

import java.lang.annotation.Annotation;
import java.util.List;

import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;

import org.bson.codecs.pojo.PropertyAccessor;
import org.bson.codecs.pojo.PropertySerialization;

public abstract class CritterPropertyModel extends PropertyModel {
    public CritterPropertyModel(EntityModel entityModel) {
        super(entityModel);
    }

    @Override
    public abstract PropertyAccessor<Object> getAccessor();

    @Override
    public abstract <A extends Annotation> A getAnnotation(Class<A> type);

    @Override
    public abstract List<Annotation> getAnnotations();

    @Override
    public abstract boolean isFinal();

    @Override
    public abstract String getFullName();

    @Override
    public abstract List<String> getLoadNames();

    @Override
    public abstract String getMappedName();

    @Override
    public abstract String getName();

    @Override
    public abstract Class<?> getNormalizedType();

    @Override
    public abstract Class<?> getType();

    @Override
    public abstract TypeData<?> getTypeData();

    @Override
    public abstract boolean isArray();

    @Override
    public abstract boolean isMap();

    @Override
    public abstract boolean isMultipleValues();

    @Override
    public abstract boolean isReference();

    @Override
    public abstract boolean isScalarValue();

    @Override
    public abstract boolean isSet();

    @Override
    public abstract boolean isTransient();

    @Override
    public abstract PropertyModel serialization(PropertySerialization serialization);
}
