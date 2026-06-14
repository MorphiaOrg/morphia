package dev.morphia.mapping.codec.pojo.critter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    /**
     * Registers all annotations from the entity's field (walking the class hierarchy) into this model.
     * Called from generated subclass constructors so non-Morphia annotations (e.g. @NonNull) are also recorded.
     */
    public static void registerFieldAnnotations(PropertyModel model, Class<?> entityClass, String fieldName) {
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                for (Annotation ann : field.getDeclaredAnnotations()) {
                    model.annotation(ann);
                }
                return;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
    }

    /**
     * Registers all annotations from the entity's getter method (walking the class hierarchy) into this model.
     * Called from generated subclass constructors so non-Morphia annotations on getters are also recorded.
     */
    public static void registerMethodAnnotations(PropertyModel model, Class<?> entityClass, String getterName) {
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(getterName) && m.getParameterCount() == 0 && !m.isBridge()) {
                    for (Annotation ann : m.getDeclaredAnnotations()) {
                        model.annotation(ann);
                    }
                    return;
                }
            }
            current = current.getSuperclass();
        }
    }

    @Override
    public abstract PropertyAccessor<Object> getAccessor();

    @Override
    public final PropertyModel accessor(PropertyAccessor<? super Object> accessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract boolean isFinal();

    @Override
    public PropertyModel isFinal(boolean isFinal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract String getFullName();

    @Override
    public abstract List<String> getLoadNames();

    @Override
    public abstract String getMappedName();

    @Override
    public PropertyModel mappedName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract String getName();

    @Override
    public PropertyModel name(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract Class<?> getNormalizedType();

    @Override
    public abstract Class<?> getType();

    @Override
    public abstract TypeData<?> getTypeData();

    @Override
    public PropertyModel typeData(TypeData<?> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return super.hasAnnotation(type);
    }

    @Override
    public abstract boolean isArray();

    @Override
    public abstract boolean isMap();

    @Override
    public abstract boolean isReference();

    @Override
    public abstract boolean isSet();

    @Override
    public abstract boolean isTransient();

    @Override
    public PropertyModel serialization(PropertySerialization serialization) {
        return super.serialization(serialization);
    }
}
