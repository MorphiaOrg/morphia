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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Text;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.morphia.utils.IndexType.fromValue;
import static java.util.Collections.emptyList;

final class IndexHelper {
    private static final Logger LOG = LoggerFactory.getLogger(IndexHelper.class);

    private final Mapper mapper;
    private final MongoDatabase database;

    IndexHelper(final Mapper mapper, final MongoDatabase database) {
        this.mapper = mapper;
        this.database = database;
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
                   .fields(Collections.singletonList(new FieldBuilder()
                                                                .value(nameToStore)
                                                                .type(IndexType.TEXT)
                                                                .weight(text.value())));
    }

    Index convert(final Indexed indexed, final String nameToStore) {
        List<Field> fields = Collections.singletonList(new FieldBuilder()
                                                                  .value(nameToStore)
                                                                  .type(fromValue(indexed.value().toIndexValue())));
        return new IndexBuilder()
                   .options(indexed.options())
                   .fields(fields);
    }

    private List<Index> collectFieldIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<>();
        for (final MappedField mf : mc.getFields()) {
            if (mf.hasAnnotation(Indexed.class)) {
                list.add(convert(mf.getAnnotation(Indexed.class), mf.getMappedFieldName()));
            } else if (mf.hasAnnotation(Text.class)) {
                list.add(convert(mf.getAnnotation(Text.class), mf.getMappedFieldName()));
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

        return indexes;
    }

    private List<Index> collectTopLevelIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<>();
        if (mc != null) {
            final List<Indexes> annotations = mc.getAnnotations(Indexes.class);
            if (annotations != null) {
                for (final Indexes indexes : annotations) {
                    for (final Index index : indexes.value()) {
                        List<Field> fields = new ArrayList<>();
                        for (Field field : index.fields()) {
                            fields.add(new FieldBuilder()
                                           .value(findField(mc, index.options(), field.value()))
                                           .type(field.type())
                                           .weight(field.weight()));
                        }

                        list.add(replaceFields(index, fields));
                    }
                }
            }
            list.addAll(collectTopLevelIndexes(mc.getSuperClass()));
        }

        return list;
    }

    private Index replaceFields(final Index original, final List<Field> list) {
        return new IndexBuilder(original)
                   .fields(list);
    }

    Document calculateKeys(final MappedClass mc, final Index index) {
        Document keys = new Document();
        for (Field field : index.fields()) {
            String path;
            try {
                path = findField(mc, index.options(), field.value());
            } catch (Exception e) {
                path = field.value();
                if (!index.options().disableValidation()) {
                    throw new MappingException(Sofia.invalidIndexPath(path, mc.getType().getName()));
                }
                LOG.warn(Sofia.invalidIndexPath(path, mc.getType().getName()));
            }
            keys.putAll(new Document(path, field.type().toIndexValue()));
        }
        return keys;
    }

    com.mongodb.client.model.IndexOptions convert(final IndexOptions options) {
        com.mongodb.client.model.IndexOptions indexOptions = new com.mongodb.client.model.IndexOptions()
                                                                 .background(options.background())
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

    String findField(final MappedClass mc, final IndexOptions options, final String path) {
        if (path.equals("$**")) {
            return path;
        }

        return new PathTarget(mapper, mc, path, !options.disableValidation()).translatedPath();
    }

    void createIndex(final MongoCollection collection, final MappedClass mc) {
        if (!mc.isInterface() && !mc.isAbstract()) {
            for (Index index : collectIndexes(mc, Collections.emptyList())) {
                createIndex(collection, mc, index);
            }
        }
    }

    void createIndex(final MongoCollection collection, final MappedClass mc, final Index index) {
        Document keys = calculateKeys(mc, index);
        com.mongodb.client.model.IndexOptions indexOptions = convert(index.options());
        calculateWeights(index, indexOptions);

        collection.createIndex(keys, indexOptions);
    }
}
