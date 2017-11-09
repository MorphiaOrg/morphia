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
import org.bson.Document;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mongodb.morphia.AnnotationBuilder.toMap;
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

    private void calculateWeights(final Index index, final com.mongodb.client.model.IndexOptions indexOptions) {
        Document weights = new Document();
        for (Field field : index.fields()) {
            if (field.weight() != -1) {
                if (field.type() != IndexType.TEXT) {
                    throw new MappingException("Weight values only apply to text indexes: " + Arrays.toString(index.fields()));
                }
                weights.put(field.value(), field.weight());
            }
        }
        if (!weights.isEmpty()) {
            indexOptions.weights(weights);
        }
    }

    Index convert(final Text text, final String nameToStore) {
        return new IndexBuilder()
            .options(text.options())
            .fields(Collections.<Field>singletonList(new FieldBuilder()
                                                         .value(nameToStore)
                                                         .type(IndexType.TEXT)
                                                         .weight(text.value())));
    }

    @SuppressWarnings("deprecation")
    Index convert(final Indexed indexed, final String nameToStore) {
        if (indexed.dropDups() || indexed.options().dropDups()) {
            LOG.warning("Support for dropDups has been removed from the server.  Please remove this setting.");
        }
        final Map<String, Object> newOptions = extractOptions(indexed.options());
        if (!extractOptions(indexed).isEmpty() && !newOptions.isEmpty()) {
            throw new MappingException("Mixed usage of deprecated @Indexed values with the new @IndexOption values is not "
                                           + "allowed.  Please migrate all settings to @IndexOptions");
        }

        List<Field> fields = Collections.<Field>singletonList(new FieldBuilder()
                                                                  .value(nameToStore)
                                                                  .type(fromValue(indexed.value().toIndexValue())));
        return newOptions.isEmpty()
               ? new IndexBuilder()
                   .options(new IndexOptionsBuilder()
                                .migrate(indexed))
                   .fields(fields)
               : new IndexBuilder()
                   .options(indexed.options())
                   .fields(fields);
    }

    @SuppressWarnings("deprecation")
    private List<Index> collectFieldIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<Index>();
        for (final MappedField mf : mc.getPersistenceFields()) {
            if (mf.hasAnnotation(Indexed.class)) {
                final Indexed indexed = mf.getAnnotation(Indexed.class);
                list.add(convert(indexed, mf.getNameToStore()));
            } else if (mf.hasAnnotation(Text.class)) {
                final Text text = mf.getAnnotation(Text.class);
                list.add(convert(text, mf.getNameToStore()));
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

                final List<MappedClass> parents = new ArrayList<MappedClass>(parentMCs);
                parents.add(mc);

                List<MappedClass> classes = new ArrayList<MappedClass>();
                MappedClass mappedClass = mapper.getMappedClass(mf.isSingleValue() ? mf.getType() : mf.getSubClass());
                classes.add(mappedClass);
                if (mappedClass.isInterface() || mappedClass.isAbstract()) {
                    classes.addAll(mapper.getSubTypes(mappedClass));
                }
                for (MappedClass aClass : classes) {
                    for (Index index : collectIndexes(aClass, parents)) {
                        List<Field> fields = new ArrayList<Field>();
                        for (Field field : index.fields()) {
                            fields.add(new FieldBuilder()
                                           .value(field.value().equals("$**")
                                                  ? field.value()
                                                  : mf.getNameToStore() + "." + field.value())
                                           .type(field.type())
                                           .weight(field.weight()));
                        }
                        list.add(new IndexBuilder(index)
                                     .fields(fields));
                    }
                }
            }
        }

        return list;
    }

    private List<Index> collectTopLevelIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<Index>();
        if (mc != null) {
            final List<Indexes> annotations = mc.getAnnotations(Indexes.class);
            if (annotations != null) {
                for (final Indexes indexes : annotations) {
                    for (final Index index : indexes.value()) {
                        Index updated = index;
                        if (index.fields().length == 0) {
                            LOG.warning(format("This index on '%s' is using deprecated configuration options.  Please update to use the "
                                                   + "fields value on @Index: %s", mc.getClazz().getName(), index.toString()));
                            updated = new IndexBuilder()
                                .migrate(index);
                        }
                        List<Field> fields = new ArrayList<Field>();
                        for (Field field : updated.fields()) {
                            fields.add(new FieldBuilder()
                                           .value(findField(mc, index.options(), asList(field.value().split("\\."))))
                                           .type(field.type())
                                           .weight(field.weight()));
                        }

                        list.add(replaceFields(updated, fields));
                    }
                }
            }
            list.addAll(collectTopLevelIndexes(mc.getSuperClass()));
        }

        return list;
    }

    private Map<String, Object> extractOptions(final IndexOptions options) {
        return toMap(options);
    }

    private Map<String, Object> extractOptions(final Indexed indexed) {
        Map<String, Object> map = toMap(indexed);
        if (indexed.options().collation().locale().equals("")) {
            map.remove("options");
        }
        map.remove("value");
        return map;
    }

    private MappingException pathFail(final MappedClass mc, final List<String> path) {
        return new MappingException(format("Could not resolve path '%s' against '%s'.", join(path, '.'), mc.getClazz().getName()));
    }

    private Index replaceFields(final Index original, final List<Field> list) {
        return new IndexBuilder(original)
            .fields(list);
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

    BsonDocument calculateKeys(final MappedClass mc, final Index index) {
        BsonDocument keys = new BsonDocument();
        for (Field field : index.fields()) {
            String path;
            try {
                path = findField(mc, index.options(), new ArrayList<String>(asList(field.value().split("\\."))));
            } catch (Exception e) {
                path = field.value();
                String message = format("The path '%s' can not be validated against '%s' and may represent an invalid index",
                                        path, mc.getClazz().getName());
                if (!index.options().disableValidation()) {
                    throw new MappingException(message);
                }
                LOG.warning(message);
            }
            keys.putAll(toBsonDocument(path, field.type().toIndexValue()));
        }
        return keys;
    }

    @SuppressWarnings("deprecation")
    com.mongodb.client.model.IndexOptions convert(final IndexOptions options, final boolean background) {
        if (options.dropDups()) {
            LOG.warning("Support for dropDups has been removed from the server.  Please remove this setting.");
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
        if (!options.partialFilter().equals("")) {
            indexOptions.partialFilterExpression(Document.parse(options.partialFilter()));
        }
        if (!options.collation().locale().equals("")) {
            indexOptions.collation(convert(options.collation()));
        }

        return indexOptions;
    }

    com.mongodb.client.model.Collation convert(final Collation collation) {
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

    String findField(final MappedClass mc, final IndexOptions options, final List<String> path) {
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

    void createIndex(final MongoCollection collection, final MappedClass mc, final boolean background) {
        if (!mc.isInterface() && !mc.isAbstract()) {
            for (Index index : collectIndexes(mc, Collections.<MappedClass>emptyList())) {
                createIndex(collection, mc, index, background);
            }
        }
    }

    void createIndex(final MongoCollection collection, final MappedClass mc, final Index index, final boolean background) {
        Index normalized = IndexBuilder.normalize(index);

        BsonDocument keys = calculateKeys(mc, normalized);
        com.mongodb.client.model.IndexOptions indexOptions = convert(normalized.options(), background);
        calculateWeights(normalized, indexOptions);

        collection.createIndex(keys, indexOptions);
    }
}
