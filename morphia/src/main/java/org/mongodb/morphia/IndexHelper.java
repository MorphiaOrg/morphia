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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.mongodb.morphia.annotations.Collation;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.annotations.Text;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.utils.IndexType;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mongodb.morphia.utils.IndexType.fromValue;

final class IndexHelper {
    private static final Logger LOG = MorphiaLoggerFactory.get(IndexHelper.class);
    private static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();

    private final Mapper mapper;
    private final MongoDatabase database;

    IndexHelper(final Mapper mapper, final MongoDatabase database) {
        this.mapper = mapper;
        this.database = database;
    }

    static Index synthesizeIndex(final String fields, final String name, final boolean unique) {
        Map<String, Object> optionsMap = new HashMap<String, Object>();
        optionsMap.put("name", name != null ? name : "");
        optionsMap.put("unique", unique);

        Map<String, Object> indexMap = new HashMap<String, Object>();
        indexMap.put("fields", parseFieldsString(fields).toArray(new Field[0]));
        indexMap.put("options", annotationForMap(IndexOptions.class, optionsMap));

        return annotationForMap(Index.class, indexMap);
    }

    static Index synthesizeIndex(final IndexOptions options, final List<Field> list) {
        Map<String, Object> indexMap = new HashMap<String, Object>();
        indexMap.put("fields", list.toArray(new Field[0]));
        indexMap.put("options", options);
        return annotationForMap(Index.class, indexMap);
    }

    static Index synthesizeIndex(final Indexed indexed, final List<Field> list) {
        Map<String, Object> indexMap = new HashMap<String, Object>();
        indexMap.put("fields", list.toArray(new Field[0]));
        indexMap.put("options", annotationForMap(IndexOptions.class, toMap(indexed)));
        return annotationForMap(Index.class, indexMap);
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

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> Map<String, Object> toMap(final T annotation) {
        final Map<String, Object> values = new HashMap<String, Object>();

        try {
            Class<T> annotationType = annotation instanceof Proxy
                                      ? extractType(Proxy.getInvocationHandler(annotation))
                                      : (Class<T>) annotation.getClass();
            for (Method method : annotationType.getDeclaredMethods()) {
                Object value = method.invoke(annotation);
                if (!method.getDefaultValue().equals(value)) {
                    values.put(method.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new MappingException(e.getMessage(), e);
        }
        return values;
    }

    private static Class extractType(final InvocationHandler invocationHandler) throws NoSuchFieldException, IllegalAccessException {
        if (invocationHandler instanceof AnnotationInvocationHandler) {
            return ((AnnotationInvocationHandler) invocationHandler).type;
        }

        java.lang.reflect.Field type = invocationHandler.getClass().getDeclaredField("type");
        type.setAccessible(true);
        return (Class) type.get(invocationHandler);
    }

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

            fields.add(synthesizeField(s, dir, -1));
        }
        return fields;
    }

    private static Field synthesizeField(final String name, final IndexType direction, final int weight) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", name);
        map.put("type", direction);
        map.put("weight", weight);

        return annotationForMap(Field.class, map);
    }

    private static String join(final List<String> path, final char delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String element : path) {
            if (builder.length() != 0) {
                builder.append(delimiter);
            }
            builder.append(element);
        }
        return builder.toString();
    }

    private BsonDocument calculateKeys(final MappedClass mc, final Index index) {
        BsonDocument keys = new BsonDocument();
        for (Field field : index.fields()) {
            if (field.weight() != -1) {
                if (field.type() != IndexType.TEXT) {
                    throw new MappingException("Weight values only apply to text indexes: " + Arrays.toString(index.fields()));
                }
            }
            String path;
            try {
                path = findField(mc, index.options(), new ArrayList<String>(asList(field.value().split("\\."))));
            } catch (Exception e) {
                path = field.value();
                LOG.warning(format("The path '%s' can not be validated against '%s' and may represent an invalid index",
                                   path, mc.getClazz().getName()));
            }
            keys.putAll(toBsonDocument(path, field.type().toIndexValue()));
        }
        return keys;
    }

    @SuppressWarnings("deprecation")
    private List<Index> collectFieldIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<Index>();
        for (final MappedField mf : mc.getPersistenceFields()) {
            if (mf.hasAnnotation(Indexed.class)) {
                final Indexed indexed = mf.getAnnotation(Indexed.class);
                if (indexed.options().dropDups()) {
                    LOG.warning("dropDups value is no longer supported by the server.  Please set this value to false and "
                                    + "validate your system behaves as expected.");
                }
                final Map<String, Object> newOptions = extractOptions(indexed.options());
                if (!extractOptions(indexed).isEmpty() && !newOptions.isEmpty()) {
                    throw new MappingException("Mixed usage of deprecated @Indexed values with the new @IndexOption values is not "
                                                   + "allowed.  Please migrate all settings to @IndexOptions");
                }

                List<Field> fields = singletonList(synthesizeField(mf.getNameToStore(), fromValue(indexed.value().toIndexValue()), -1));
                list.add(newOptions.isEmpty() ? synthesizeIndex(indexed, fields) : synthesizeIndex(indexed.options(), fields));
            } else if (mf.hasAnnotation(Text.class)) {
                final Text text = mf.getAnnotation(Text.class);

                list.add(synthesizeIndex(text.options(),
                                         singletonList(synthesizeField(mf.getNameToStore(), IndexType.TEXT, text.value()))));
            }
        }
        return list;
    }

    private List<Index> collectIndexes(final MappedClass mc, final List<MappedClass> parentMCs) {
        if (parentMCs.contains(mc) || mc.getEmbeddedAnnotation() != null && parentMCs.isEmpty()) {
            return emptyList();
        }

        List<Index> indexes = collectTopLevelIndexes(mc);
        indexes.addAll(collectFieldIndexes(mc));
        indexes.addAll(collectNestedIndexes(mc, parentMCs));

        return indexes;
    }

    private List<Index> collectNestedIndexes(final MappedClass mc, final List<MappedClass> parentMCs) {
        List<Index> list = new ArrayList<Index>();
        for (final MappedField mf : mc.getPersistenceFields()) {
            if (!mf.isTypeMongoCompatible() && !mf.hasAnnotation(Reference.class) && !mf.hasAnnotation(Serialized.class)
                && !mf.hasAnnotation(NotSaved.class) && !mf.isTransient()) {
                final List<MappedClass> newParentClasses = new ArrayList<MappedClass>(parentMCs);
                newParentClasses.add(mc);
                List<Index> indexes =
                    collectIndexes(mapper.getMappedClass(mf.isSingleValue() ? mf.getType() : mf.getSubClass()), newParentClasses);
                for (Index index : indexes) {
                    List<Field> fields = new ArrayList<Field>();
                    for (Field field : index.fields()) {
                        fields.add(synthesizeField(field.value().equals("$**")
                                                   ? field.value()
                                                   : mf.getNameToStore() + "." + field.value(),
                                                   field.type(), field.weight()));
                    }
                    list.add(replaceFields(index, fields));
                }
            }
        }

        return list;
    }

    private List<Index> collectTopLevelIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<Index>();
        final List<Indexes> annotations = mc.getAnnotations(Indexes.class);
        if (annotations != null) {
            for (final Indexes indexes : annotations) {
                for (final Index index : indexes.value()) {
                    Index updated = index;
                    if (index.fields().length == 0) {
                        LOG.warning(format("This index on '%s' is using deprecated configuration options.  Please update to use the "
                                               + "fields value on @Index: %s", mc.getClazz().getName(), index.toString()));
                        updated = synthesizeIndexFromOldFormat(index);
                    }
                    List<Field> fields = new ArrayList<Field>();
                    for (Field field : updated.fields()) {
                        String path = findField(mc, index.options(), asList(field.value().split("\\.")));
                        fields.add(synthesizeField(path, field.type(), field.weight()));
                    }

                    list.add(replaceFields(updated, fields));
                }
            }
        }
        return list;
    }

    @SuppressWarnings("deprecation")
    private com.mongodb.client.model.IndexOptions convert(final IndexOptions options, final boolean background) {
        if (options.dropDups()) {
            LOG.warning("dropDups value is no longer supported by the server.  Please set this value to false and "
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
        if (!options.collation().locale().equals("")) {
            indexOptions.collation(convert(options.collation()));
        }

        return indexOptions;
    }

    private com.mongodb.client.model.Collation convert(final Collation collation) {
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

    private String createIndex(final MongoCollection collection, final BsonDocument keys,
                               final com.mongodb.client.model.IndexOptions options) {
        return collection.createIndex(keys, options);
    }

    private Map<String, Object> extractOptions(final IndexOptions options) {
        return toMap(options);
    }

    private Map<String, Object> extractOptions(final Indexed indexed) {
        Map<String, Object> map = toMap(indexed);
        if (indexed.options().collation().locale().equals("")) {
            map.remove("options");
        }
        return map;
    }

    private String findField(final MappedClass mc, final IndexOptions options, final List<String> path) {
        String segment = path.get(0);
        if (segment.equals("$**")) {
            return segment;
        }

        MappedField mf = mc.getMappedField(segment);
        if (mf == null) {
            mf = mc.getMappedFieldByJavaField(segment);
        }
        if (mf == null && mc.isInterface()) {
            for (final MappedClass mappedClass : mapper.getSubTypes(mc)) {
                try {
                    return findField(mappedClass, options, new ArrayList<String>(path));
                } catch (MappingException e) {
                    // try the next one
                }
            }
        }
        String namePath;
        if (mf != null) {
            namePath = mf.getNameToStore();
        } else {
            if (!options.disableValidation()) {
                throw pathFail(mc, path);
            } else {
                return join(path, '.');
            }
        }
        if (path.size() > 1) {
            try {
                Class concreteType = !mf.isSingleValue() ? mf.getSubClass() : mf.getConcreteType();
                namePath += "." + findField(mapper.getMappedClass(concreteType), options, path.subList(1, path.size()));
            } catch (MappingException e) {
                if (!options.disableValidation()) {
                    throw pathFail(mc, path);
                } else {
                    return join(path, '.');
                }
            }
        }
        return namePath;
    }

    private MappingException pathFail(final MappedClass mc, final List<String> path) {
        return new MappingException(format("Could not resolve path '%s' against '%s'.", join(path, '.'), mc.getClazz().getName()));
    }

    private Index replaceFields(final Index original, final List<Field> list) {
        Map<String, Object> indexMap = toMap(original);
        indexMap.put("fields", list.toArray(new Field[0]));

        return annotationForMap(Index.class, indexMap);
    }

    @SuppressWarnings("deprecation")
    private Index synthesizeIndexFromOldFormat(final Index index) {
        return synthesizeIndex(annotationForMap(IndexOptions.class, toMap(index)), parseFieldsString(index.value()));
    }

    @SuppressWarnings("unchecked")
    private BsonDocument toBsonDocument(final String key, final Object value) {
        BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
        writer.writeStartDocument();
        writer.writeName(key);
        ((Encoder) database.getCodecRegistry().get(value.getClass())).encode(writer, value, ENCODER_CONTEXT);
        writer.writeEndDocument();
        return writer.getDocument();
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
                return toString();
            }
            return values.get(method.getName());
        }

        @Override
        public String toString() {
            return format("%s %s", type.getSimpleName(), values.toString());
        }
    }

    void createIndex(final MongoCollection collection, final MappedClass mc, final boolean background) {

        for (Index index : collectIndexes(mc, Collections.<MappedClass>emptyList())) {
            com.mongodb.client.model.IndexOptions options = convert(index.options(), background);
            BsonDocument keys = new BsonDocument();
            for (Field field : index.fields()) {
                if (field.weight() != -1) {
                    if (field.type() != IndexType.TEXT) {
                        throw new MappingException("Weight values only apply to text indexes: " + Arrays.toString(index.fields()));
                    }
                    BsonDocument weights = (BsonDocument) options.getWeights();
                    if (weights == null) {
                        weights = new BsonDocument();
                        options.weights(weights);
                    }
                    weights.putAll(toBsonDocument(field.value(), field.weight()));
                }

                keys.putAll(toBsonDocument(field.value(), field.type().toIndexValue()));
            }

            createIndex(collection, keys, options);
        }
    }

    void createIndex(final MappedClass mc, final MongoCollection collection, final Index index, final boolean background) {
        createIndex(collection, calculateKeys(mc, index), convert(index.options(), background));
    }

}
