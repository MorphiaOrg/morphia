package dev.morphia.annotations.builders;

import dev.morphia.mapping.MappingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @param <T>
 * @morphia.internal
 */
public abstract class AnnotationBuilder<T extends Annotation> implements Annotation {
    private final Map<String, Object> values = new HashMap<String, Object>();

    protected AnnotationBuilder() {
        for (Method method : annotationType().getDeclaredMethods()) {
            values.put(method.getName(), method.getDefaultValue());
        }
    }

    protected AnnotationBuilder(T original) {
        try {
            for (Method method : annotationType().getDeclaredMethods()) {
                values.put(method.getName(), method.invoke(original));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <V> V get(String key) {
        return (V) values.get(key);
    }

    protected void put(String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    void putAll(Map<String, Object> map) {
        values.putAll(map);
    }

    @Override
    public String toString() {
        return format("@%s %s", annotationType().getName(), values);
    }

    @Override
    public abstract Class<T> annotationType();

    @SuppressWarnings("unchecked")
    static <A extends Annotation> Map<String, Object> toMap(A annotation) {
        final Map<String, Object> map = new HashMap<String, Object>();
        try {
            Class<A> annotationType = (Class<A>) annotation.annotationType();
            for (Method method : annotationType.getDeclaredMethods()) {
                Object value = unwrapAnnotation(method.invoke(annotation));
                final Object defaultValue = unwrapAnnotation(method.getDefaultValue());
                if (value != null && !value.equals(defaultValue)) {
                    map.put(method.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new MappingException(e.getMessage(), e);
        }
        return map;
    }

    private static Object unwrapAnnotation(Object o) {
        if (o instanceof Annotation) {
            return toMap((Annotation) o);
        } else {
            return o;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnnotationBuilder)) {
            return false;
        }

        return values.equals(((AnnotationBuilder<?>) o).values);

    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
