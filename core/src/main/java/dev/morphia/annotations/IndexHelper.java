package dev.morphia.annotations;

import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.sofia.Sofia;
import dev.morphia.utils.IndexType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dev.morphia.annotations.builders.FieldBuilder.fieldBuilder;
import static dev.morphia.annotations.builders.IndexBuilder.indexBuilder;
import static dev.morphia.utils.IndexType.fromValue;
import static java.util.Collections.emptyList;

/**
 * A helper class for dealing with index definitions
 *
 * @morphia.internal
 * @since 2.0
 */
public final class IndexHelper {
    private static final Logger LOG = LoggerFactory.getLogger(IndexHelper.class);

    private final Mapper mapper;

    /**
     * @param mapper the mapper
     * @morphia.internal
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public IndexHelper(Mapper mapper) {
        this.mapper = mapper;
    }

    public Document calculateKeys(EntityModel entityModel, Index index) {
        Document keys = new Document();
        for (Field field : index.fields()) {
            String path;
            try {
                path = findField(entityModel, index.options(), field.value());
            } catch (Exception e) {
                path = field.value();
                if (!index.options().disableValidation()) {
                    throw new MappingException(Sofia.invalidIndexPath(path, entityModel.getType().getName()));
                }
                LOG.warn(Sofia.invalidIndexPath(path, entityModel.getType().getName()));
            }
            keys.putAll(new Document(path, field.type().toIndexValue()));
        }
        return keys;
    }

    @Nullable
    public Index convert(@Nullable Text text, String nameToStore) {
        return text == null
               ? null
               : indexBuilder()
                   .options(text.options())
                   .fields(fieldBuilder()
                       .value(nameToStore)
                       .type(IndexType.TEXT)
                       .weight(text.value())
                       .build())
                     .build();
    }

    @Nullable
    public Index convert(@Nullable Indexed indexed, String nameToStore) {
        return indexed == null
               ? null
               : indexBuilder()
                   .options(indexed.options())
                   .fields(fieldBuilder()
                       .value(nameToStore)
                       .type(fromValue(indexed.value().toIndexValue()))
                       .build())
                     .build();
    }

    public com.mongodb.client.model.IndexOptions convert(IndexOptions options) {
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

    public com.mongodb.client.model.Collation convert(Collation collation) {
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

    /**
     * @param collection the collection
     * @param model      the model
     * @morphia.internal
     */
    public void createIndex(MongoCollection<?> collection, EntityModel model) {
        if (!model.isInterface() && !model.isAbstract()) {
            for (Index index : collectIndexes(model, Collections.emptyList())) {
                createIndex(collection, model, index);
            }
        }
    }

    public void createIndex(MongoCollection<?> collection, EntityModel entityModel, Index index) {
        Document keys = calculateKeys(entityModel, index);
        com.mongodb.client.model.IndexOptions indexOptions = convert(index.options());
        calculateWeights(index, indexOptions);

        collection.createIndex(keys, indexOptions);
    }

    public String findField(EntityModel entityModel, IndexOptions options, String path) {
        if (path.equals("$**")) {
            return path;
        }

        return new PathTarget(mapper, entityModel, path, !options.disableValidation()).translatedPath();
    }

    private void calculateWeights(Index index, com.mongodb.client.model.IndexOptions indexOptions) {
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

    private List<Index> collectFieldIndexes(EntityModel entityModel) {
        List<Index> list = entityModel.getProperties(Indexed.class).stream()
                                      .map(field -> convert(field.getAnnotation(Indexed.class), field.getMappedName()))
                                      .collect(Collectors.toList());

        list.addAll(entityModel.getProperties(Text.class).stream()
                               .map(field -> convert(field.getAnnotation(Text.class), field.getMappedName()))
                               .collect(Collectors.toList()));

        return list;
    }

    private List<Index> collectIndexes(EntityModel entityModel, List<EntityModel> parentModels) {
        if (parentModels.contains(entityModel) || entityModel.getEmbeddedAnnotation() != null && parentModels.isEmpty()) {
            return emptyList();
        }

        List<Index> indexes = collectTopLevelIndexes(entityModel);
        indexes.addAll(collectFieldIndexes(entityModel));

        return indexes;
    }

    private List<Index> collectTopLevelIndexes(EntityModel entityModel) {
        List<Index> list = new ArrayList<>();
        final Indexes indexes = entityModel.getAnnotation(Indexes.class);
        if (indexes != null) {
            for (Index index : indexes.value()) {
                List<Field> fields = new ArrayList<>();
                for (Field field : index.fields()) {
                    fields.add(fieldBuilder()
                                   .value(findField(entityModel, index.options(), field.value()))
                                   .type(field.type())
                                   .weight(field.weight())
                                   .build());
                }

                list.add(replaceFields(index, fields));
            }
        }
        EntityModel superClass = entityModel.getSuperClass();
        if (superClass != null) {
            list.addAll(collectTopLevelIndexes(superClass));
        }

        return list;
    }

    private Index replaceFields(Index original, List<Field> list) {
        return indexBuilder(original)
                   .fields(list.toArray(new Field[0]))
                   .build();
    }
}
