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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.annotations.Collation;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.utils.IndexType;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;

final class AnnotationUtils {
    private AnnotationUtils() {

    }

    @SuppressWarnings("deprecation")
    protected static com.mongodb.client.model.IndexOptions convert(final IndexOptions options, final boolean background) {
        if (options.dropDups()) {
            throw new MappingException("dropDups value has been desupported by the server.  Please set this value to false and "
                                           + "validate your system behaves as expected.");
        }
        com.mongodb.client.model.IndexOptions indexOptions = new com.mongodb.client.model.IndexOptions()
            .background(options.background() || background)
            .sparse(options.sparse())
            .unique(options.unique());

        if (!options.language().equals("")) {
            indexOptions.defaultLanguage(options.language());
        }
        if (!options.languageOverride().equals("")) {
            indexOptions.languageOverride(options.languageOverride());
        }
        if (!options.name().equals("")) {
            indexOptions.name(options.name());
        }
        if (options.expireAfterSeconds() != -1) {
            indexOptions.expireAfter((long) options.expireAfterSeconds(), TimeUnit.SECONDS);
        }
        if (options.expireAfterSeconds() != -1) {
            indexOptions.expireAfter((long) options.expireAfterSeconds(), TimeUnit.SECONDS);
        }
        if (!options.collation().locale().equals("")) {
            indexOptions.collation(convert(options.collation()));
        }

        return indexOptions;
    }

    private static com.mongodb.client.model.Collation convert(final Collation collation) {
        return com.mongodb.client.model.Collation.builder()
                                                 .locale(collation.locale())
                                                 .backwards(collation.backwards())
                                                 .caseLevel(collation.caseLevel())
                                                 .collationAlternate(collation.alternate())
                                                 .collationCaseFirst(collation.caseFirst())
                                                 .collationMaxVariable(collation.maxVariable())
                                                 .collationStrength(collation.strength())
                                                 .normalization(collation.normalization())
                                                 .numericOrdering(collation.numericOrdering())
                                                 .build();
    }

    @SuppressWarnings("deprecation")
    static Index synthesizeIndex(final Index index) {
        return synthesizeIndex(index.value(), index.name(), index.background(), index.disableValidation(),
                               index.sparse(), index.unique(), index.expireAfterSeconds(), null, null, null);
    }

    static Index synthesizeIndex(final String fields, final String name, final boolean background, final boolean disableValidation,
                                 final boolean sparse, final boolean unique, final int expireAfterSeconds, final String language,
                                 final String languageOverride, final Collation collation) {
        return synthesizeIndex(name, background, disableValidation, sparse, unique, expireAfterSeconds, language, languageOverride,
                               collation, parseFieldsString(fields));
    }

    static Index synthesizeIndex(final String name, final boolean background, final boolean disableValidation,
                                 final boolean sparse, final boolean unique, final int expireAfterSeconds, final String language,
                                 final String languageOverride, final Collation collation, final List<Field> list) {
        Map<String, Object> indexMap = new HashMap<String, Object>();

        indexMap.put("fields", list.toArray(new Field[0]));

        indexMap.put("options", synthesizeOptions(name, background, disableValidation, sparse, unique, expireAfterSeconds, language,
                                                  languageOverride, collation));

        return annotationForMap(Index.class, indexMap);
    }

    private static IndexOptions synthesizeOptions(final String name, final boolean background, final boolean disableValidation,
                                                  final boolean sparse, final boolean unique, final int expireAfterSeconds,
                                                  final String language, final String languageOverride, final Collation collation) {
        Map<String, Object> optionsMap = new HashMap<String, Object>();
        optionsMap.put("name", name != null ? name : "");
        optionsMap.put("background", background);
        optionsMap.put("disableValidation", disableValidation);
        optionsMap.put("sparse", sparse);
        optionsMap.put("unique", unique);
        optionsMap.put("expireAfterSeconds", expireAfterSeconds);
        optionsMap.put("language", language != null ? language : "");
        optionsMap.put("languageOverride", languageOverride != null ? languageOverride : "");
        optionsMap.put("collation", collation);

        return annotationForMap(IndexOptions.class, optionsMap);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T annotationForMap(final Class<T> annotationType, final Map<String, Object> valuesMap) {
        final Map<String, Object> values = new HashMap<String, Object>();

        for (Method method : annotationType.getDeclaredMethods()) {
            values.put(method.getName(), method.getDefaultValue());
        }

        for (Entry<String, Object> entry : valuesMap.entrySet()) {
            if (entry.getValue() != null) {
                values.put(entry.getKey(), entry.getValue());
            }
        }

        return (T) newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType},
                                    new AnnotationInvocationHandler(annotationType, values));
    }

    private static <T extends Annotation> Map<String, Object> toMap(final T annotation) {
        final Map<String, Object> values = new HashMap<String, Object>();

        try {
            for (Method method : annotation.getClass().getDeclaredMethods()) {
                if ((method.getParameterTypes().length == 0) && !method.getName().equals("hashCode")) {
                    values.put(method.getName(), method.invoke(annotation));
                }
            }
        } catch (Exception e) {
            throw new MappingException(e.getMessage(), e);
        }
        return values;
    }

    /**
     * Parses the string and validates each part
     *
     * @param str the String to parse
     * @return the DBObject
     */
    private static List<Field> parseFieldsString(final String str) {
        List<Field> fields = new ArrayList<Field>();
        final String[] parts = str.split(",");
        for (String s : parts) {
            s = s.trim();
            IndexType dir = IndexType.ASC;

            if (s.startsWith("-")) {
                dir = IndexType.DESC;
                s = s.substring(1).trim();
            }

            fields.add(field(s, dir, -1));
        }
        return fields;
    }

    static Field field(final String name, final IndexType direction, final int weight) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", name);
        map.put("type", direction);
        map.put("weight", weight);

        return annotationForMap(Field.class, map);
    }

    static Index replaceFields(final Index original, final List<Field> list) {
        Map<String, Object> indexMap = toMap(original);
        indexMap.put("fields", list.toArray(new Field[0]));

        return annotationForMap(Index.class, indexMap);
    }

    static DBObject extractOptions(final IndexOptions options) {
        final DBObject opts = new BasicDBObject();

        putIfNotEmpty(opts, "name", options.name());
        putIfNotEmpty(opts, "default_language", options.language());
        putIfNotEmpty(opts, "language_override", options.languageOverride());
        putIfTrue(opts, "background", options.background());
        putIfTrue(opts, "sparse", options.sparse());
        putIfTrue(opts, "unique", options.unique());
        if (options.expireAfterSeconds() != -1) {
            opts.put("expireAfterSeconds", options.expireAfterSeconds());
        }
        return opts;
    }

    @SuppressWarnings("deprecation")
    static BasicDBObject extractOptions(final Indexed indexed) {
        final BasicDBObject opts = new BasicDBObject();

        putIfNotEmpty(opts, "name", indexed.name());
        putIfTrue(opts, "background", indexed.background());
        putIfTrue(opts, "sparse", indexed.sparse());
        putIfTrue(opts, "unique", indexed.unique());
        if (indexed.expireAfterSeconds() != -1) {
            opts.put("expireAfterSeconds", indexed.expireAfterSeconds());
        }
        return opts;
    }

    private static void putIfNotEmpty(final DBObject opts, final String key, final String value) {
        if (!value.equals("")) {
            opts.put(key, value);
        }
    }

    private static void putIfTrue(final DBObject opts, final String key, final boolean value) {
        if (value) {
            opts.put(key, true);
        }
    }

    private static class AnnotationInvocationHandler<T> implements InvocationHandler {
        private final Class<T> annotationType;
        private final Map<String, Object> values;

        AnnotationInvocationHandler(final Class<T> annotationType, final Map<String, Object> values) {
            this.annotationType = annotationType;
            this.values = values;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws InvocationTargetException, IllegalAccessException {
            if (method.getName().equals("toString")) {
                return toString();
            }
            return values.get(method.getName());
        }

        @Override
        public String toString() {
            return format("%s %s", annotationType.getSimpleName(), values.toString());
        }
    }
}
