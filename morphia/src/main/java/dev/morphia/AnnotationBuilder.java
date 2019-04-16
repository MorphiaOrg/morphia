/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia;

import dev.morphia.mapping.MappingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

abstract class AnnotationBuilder<T extends Annotation> implements Annotation {
    private final Map<String, Object> values = new HashMap<String, Object>();

    AnnotationBuilder() {
        for (Method method : annotationType().getDeclaredMethods()) {
            values.put(method.getName(), method.getDefaultValue());
        }
    }

    AnnotationBuilder(final T original) {
        try {
            for (Method method : annotationType().getDeclaredMethods()) {
                values.put(method.getName(), method.invoke(original));
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    <V> V get(final String key) {
        return (V) values.get(key);
    }

    void put(final String key, final Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    void putAll(final Map<String, Object> map) {
        values.putAll(map);
    }

    @Override
    public String toString() {
        return format("@%s %s", annotationType().getName(), values.toString());
    }

    @Override
    public abstract Class<T> annotationType();

    @SuppressWarnings("unchecked")
    static <A extends Annotation> Map<String, Object> toMap(final A annotation) {
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

    private static Object unwrapAnnotation(final Object o) {
        if (o instanceof Annotation) {
            return toMap((Annotation) o);
        } else {
            return o;
        }
    }

    @Override
    public boolean equals(final Object o) {
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
