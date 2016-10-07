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

package org.mongodb.morphia;

import org.mongodb.morphia.mapping.MappingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
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
                Object value = method.invoke(original);
                if (!method.getDefaultValue().equals(value)) {
                    values.put(method.getName(), value);
                }
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
        values.put(key, value);
    }

    void putAll(final Map<String, Object> map) {
        values.putAll(map);
    }

    private static class AnnotationInvocationHandler<T> implements InvocationHandler {
        private final Class<T> type;
        private final Map<String, Object> values;

        AnnotationInvocationHandler(final Class<T> type, final Map<String, Object> values) {
            this.type = type;
            this.values = values;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws InvocationTargetException, IllegalAccessException {
            if (method.getName().equals("toString")) {
                return values.toString();
            }
            return values.get(method.getName());
        }

        @Override
        public String toString() {
            return format("%s %s", type.getSimpleName(), values.toString());
        }
    }

    @Override
    public abstract Class<T> annotationType();

    @SuppressWarnings("unchecked")
    <A extends Annotation> Map<String, Object> toMap(final A annotation) {
        if (annotation instanceof AnnotationBuilder) {
            return new HashMap<String, Object>(((AnnotationBuilder) annotation).values);
        }

        final Map<String, Object> map = new HashMap<String, Object>();
        try {
            for (Method method : annotationType().getDeclaredMethods()) {
                Object value = method.invoke(annotation);
                if (!method.getDefaultValue().equals(value)) {
                    map.put(method.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new MappingException(e.getMessage(), e);
        }
        return map;
    }
}
