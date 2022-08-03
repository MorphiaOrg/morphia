package dev.morphia;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.aggregation.codecs.AggregationCodecProvider;
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.IndexHelper;
import dev.morphia.annotations.ShardKeys;
import dev.morphia.annotations.ShardOptions;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.CollectionConfiguration;
import dev.morphia.internal.ReadConfigurable;
import dev.morphia.internal.WriteConfigurable;
import dev.morphia.mapping.EntityModelImporter;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.EnumCodecProvider;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.MorphiaTypesCodecProvider;
import dev.morphia.mapping.codec.PrimitiveCodecRegistry;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.MergingEncoder;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.CountOptions;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.Update;
import dev.morphia.query.UpdateException;
import dev.morphia.sofia.Sofia;
import dev.morphia.transactions.MorphiaSessionImpl;
import dev.morphia.transactions.MorphiaTransaction;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.sofia.Sofia.noDocumentsUpdated;
import static dev.morphia.sofia.Sofia.noShardKeyMatch;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.bson.Document.parse;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings({"unchecked", "rawtypes", "removal"})
public class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);
    private final MongoClient mongoClient;
    private final Mapper mapper;
    private final QueryFactory queryFactory;
    private final CodecRegistry codecRegistry;
    private final List<MorphiaCodecProvider> morphiaCodecProviders = new ArrayList<>();
    private MongoDatabase database;
    private DatastoreOperations operations;

    protected DatastoreImpl(Mapper mapper, MongoClient mongoClient, String dbName) {
        this.database = mongoClient.getDatabase(dbName);
        this.mapper = mapper;
        this.mongoClient = mongoClient;
        this.queryFactory = mapper.getOptions().getQueryFactory();
        if (mapper.getOptions().autoImportModels()) {
            importModels();
        }

        morphiaCodecProviders.add(new MorphiaCodecProvider(this));

        CodecRegistry codecRegistry = database.getCodecRegistry();
        List<CodecProvider> providers = new ArrayList<>();
        if (mapper.getOptions().codecProvider() != null) {
            providers.add(mapper.getOptions().codecProvider());
        }

        providers.addAll(List.of(new MorphiaTypesCodecProvider(this),
            new PrimitiveCodecRegistry(codecRegistry),
            new EnumCodecProvider(),
            new AggregationCodecProvider(this)));

        providers.addAll(morphiaCodecProviders);
        providers.add(codecRegistry);
        this.codecRegistry = fromProviders(providers);

        this.database = database.withCodecRegistry(this.codecRegistry);
        operations = new CollectionOperations();
    }

    /**
     * Copy constructor for a datastore
     *
     * @param datastore the datastore to clone
     * @morphia.internal
     * @since 2.0
     */
    protected DatastoreImpl(DatastoreImpl datastore) {
        this.database = datastore.database;
        this.mongoClient = datastore.mongoClient;
        this.mapper = datastore.mapper;
        this.queryFactory = datastore.queryFactory;
        this.codecRegistry = datastore.codecRegistry;
    }

    @Override
    public <T> void insert(T entity, InsertOneOptions options) {
        MongoCollection<T> collection = (MongoCollection<T>) configureCollection(options, getCollection(entity.getClass()));
        VersionBumpInfo info = updateVersioning(entity);

        try {
            operations.insertOne(collection, entity, options);
        } catch (MongoWriteException e) {
            info.rollbackVersion();
            throw e;
        }
    }

    @Override
    public <T> void insert(List<T> entities, InsertManyOptions options) {
        if (entities.isEmpty()) {
            return;
        }

        Map<Class<?>, List<T>> grouped = groupByType(entities, model -> false);

        String alternate = options.collection();
        if (alternate != null && grouped.size() > 1) {
            Sofia.logInsertManyAlternateCollection();
        }

        grouped.entrySet().stream()
               .forEach(entry -> {
                   List<T> list = entry.getValue();

                   List<VersionBumpInfo> infos = list.stream()
                                                     .map(this::updateVersioning)
                                                     .collect(Collectors.toList());

                   try {
                       MongoCollection<T> collection = configureCollection(options,
                           (MongoCollection<T>) getCollection(entry.getKey()));
                       operations.insertMany(collection, list, options);
                   } catch (MongoException e) {
                       infos.forEach(VersionBumpInfo::rollbackVersion);
                       throw e;
                   }
               });
    }

    @Override
    public Aggregation<Document> aggregate(String source) {
        return new AggregationImpl(this, getDatabase().getCollection(source));
    }

    @Override
    public <T> Aggregation<T> aggregate(Class<T> source) {
        return new AggregationImpl(this, source, getCollection(source));
    }

    @Override
    public dev.morphia.aggregation.AggregationPipeline createAggregation(Class source) {
        return new dev.morphia.aggregation.AggregationPipelineImpl(this, getCollection(source), source);
    }

    @Override
    public <T> dev.morphia.query.UpdateOperations<T> createUpdateOperations(Class<T> clazz) {
        return new dev.morphia.query.UpdateOpsImpl<>(this, clazz);
    }

    /**
     * Applies configuration options to the collection
     *
     * @param <T>        the collection type
     * @param options    the options to apply
     * @param collection the collection to configure
     * @return the configured collection
     * @morphia.internal
     */
    @NonNull
    @MorphiaInternal
    public <T> MongoCollection<T> configureCollection(CollectionConfiguration options, MongoCollection<T> collection) {
        if (options instanceof CollectionConfigurable) {
            collection = ((CollectionConfigurable<?>) options).prepare(collection, getDatabase());
        }
        if (options instanceof ReadConfigurable) {
            collection = ((ReadConfigurable<?>) options).prepare(collection);
        }
        if (options instanceof WriteConfigurable) {
            collection = ((WriteConfigurable<?>) options).configure(collection);
        }
        return collection;
    }

    /**
     * Deletes the given entity (by @Id), with the WriteConcern
     *
     * @param entity  the entity to delete
     * @param options the options to use when deleting
     * @return results of the delete
     */
    @Override
    public <T> DeleteResult delete(T entity, DeleteOptions options) {
        if (entity instanceof Class<?>) {
            throw new MappingException(Sofia.deleteWithClass(entity.getClass().getName()));
        }
        Object id = mapper.getId(entity);
        return id != null
               ? find(entity.getClass())
                     .filter(eq("_id", id))
                     .delete(options)
               : new NoDeleteResult();
    }

    @Override
    public <T> DeleteResult delete(T entity) {
        return delete(entity, new DeleteOptions().writeConcern(mapper.getWriteConcern(entity.getClass())));
    }

    @Override
    public void enableDocumentValidation() {
        for (EntityModel model : mapper.getMappedEntities()) {
            enableDocumentValidation(model);
        }
    }

    @Override
    public void ensureIndexes() {
        if (mapper.getMappedEntities().isEmpty()) {
            Sofia.logNoMappedClasses();
        }
        final IndexHelper indexHelper = new IndexHelper(mapper);
        for (EntityModel model : mapper.getMappedEntities()) {
            if (model.getIdProperty() != null) {
                indexHelper.createIndex(getCollection(model.getType()), model);
            }
        }
    }

    public <T> void ensureIndexes(Class<T> type) {
        EntityModel model = mapper.getEntityModel(type);
        final IndexHelper indexHelper = new IndexHelper(mapper);
        indexHelper.createIndex(getCollection(type), model);
    }

    @Override
    public <T> Query<T> find(Class<T> type) {
        return queryFactory.createQuery(this, type);
    }

    @Override
    public <T> Query<T> find(Class<T> type, Document nativeQuery) {
        return queryFactory.createQuery(this, type, nativeQuery);
    }

    @Override
    public <T> Query<T> find(String collection, Class<T> type) {
        return queryFactory.createQuery(this, collection, type);
    }

    @Override
    public <T> Query<T> find(String collection) {
        Class<T> type = mapper.getClassFromCollection(collection);
        return queryFactory.createQuery(this, type);
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    @Override
    public <T> MongoCollection<T> getCollection(Class<T> type) {
        EntityModel entityModel = mapper.getEntityModel(type);
        String collectionName = entityModel.getCollectionName();

        MongoCollection<T> collection = getDatabase().getCollection(collectionName, type);

        Entity annotation = entityModel.getEntityAnnotation();
        if (annotation != null && !annotation.concern().equals("")) {
            collection = collection.withWriteConcern(WriteConcern.valueOf(annotation.concern()));
        }
        return collection;
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * @return the logged query
     * @morphia.internal
     */
    @Override
    public String getLoggedQuery(FindOptions options) {
        if (options.isLogQuery()) {
            String json = "{}";
            Document first = getDatabase()
                                 .getCollection("system.profile")
                                 .find(new Document("command.comment", "logged query: " + options.queryLogId()),
                                     Document.class)
                                 .projection(new Document("command.filter", 1))
                                 .first();
            if (first != null) {
                Document command = (Document) first.get("command");
                Document filter = (Document) command.get("filter");
                if (filter != null) {
                    json = filter.toJson(codecRegistry.get(Document.class));
                }
            }
            return json;
        } else {
            throw new IllegalStateException(Sofia.queryNotLogged());
        }
    }

    @Override
    public void ensureCaps() {
        List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        for (EntityModel model : mapper.getMappedEntities()) {
            Entity entityAnnotation = model.getEntityAnnotation();
            if (entityAnnotation != null) {
                CappedAt cappedAt = entityAnnotation.cap();
                if (cappedAt.value() > 0 || cappedAt.count() > 0) {
                    final CappedAt cap = entityAnnotation.cap();
                    final String collName = model.getCollectionName();
                    final CreateCollectionOptions dbCapOpts = new CreateCollectionOptions()
                                                                  .capped(true);
                    if (cap.value() > 0) {
                        dbCapOpts.sizeInBytes(cap.value());
                    }
                    if (cap.count() > 0) {
                        dbCapOpts.maxDocuments(cap.count());
                    }
                    final MongoDatabase database = getDatabase();
                    if (collectionNames.contains(collName)) {
                        final Document dbResult = database.runCommand(new Document("collstats", collName));
                        if (dbResult.getBoolean("capped", false)) {
                            LOG.debug("MongoCollection already exists and is capped already; doing nothing. " + dbResult);
                        } else {
                            LOG.warn("MongoCollection already exists with same name(" + collName
                                     + ") and is not capped; not creating capped version!");
                        }
                    } else {
                        getDatabase().createCollection(collName, dbCapOpts);
                        LOG.debug("Created capped MongoCollection (" + collName + ") with opts " + dbCapOpts);
                    }
                }
            }
        }
    }

    @Override
    public <T> T merge(T entity, InsertOneOptions options) {
        final Object id = mapper.getId(entity);
        if (id == null) {
            throw new MappingException("Could not get id for " + entity.getClass().getName());
        }

        VersionBumpInfo info = updateVersioning(entity);

        final Query<T> query = (Query<T>) find(entity.getClass()).filter(eq("_id", id));
        info.filter(query);

        Update<T> update;
        if (!options.unsetMissing()) {
            update = query.update(set(entity));
        } else {
            update = ((MergingEncoder<T>) new MergingEncoder(query,
                (MorphiaCodec) codecRegistry.get(entity.getClass())))
                         .encode(entity);
        }
        UpdateResult execute = update.execute(new UpdateOptions()
                                                  .writeConcern(options.writeConcern()));
        if (execute.getModifiedCount() != 1) {
            if (info.versioned()) {
                info.rollbackVersion();
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw new UpdateException("Nothing updated");
        }

        return (T) find(entity.getClass()).filter(eq("_id", id)).iterator(new FindOptions().limit(1)).next();
    }

    @Override
    public <T> T replace(T entity, ReplaceOptions options) {
        MongoCollection collection = configureCollection(options, getCollection(entity.getClass()));

        EntityModel entityModel = mapper.getEntityModel(entity.getClass());
        Object id = entityModel.getIdProperty().getValue(entity);
        if (id == null) {
            throw new MissingIdException();
        }
        VersionBumpInfo info = updateVersioning(entity);

        try {
            Document filter = new Document("_id", id);
            info.filter(filter);
            entityModel.getShardKeys().forEach((property) -> {
                filter.put(property.getMappedName(), property.getValue(entity));
            });

            UpdateResult updateResult = operations.replaceOne(collection, entity, filter, options);

            if (updateResult.getModifiedCount() != 1) {
                if (info.versioned()) {
                    info.rollbackVersion();
                    throw new VersionMismatchException(entity.getClass(), id);
                } else if (!entityModel.getShardKeys().isEmpty()) {
                    throw new MappingException(noShardKeyMatch(entityModel.getShardKeys()
                                                                          .stream().map(PropertyModel::getMappedName)
                                                                          .collect(joining(", "))));
                } else {
                    throw new MappingException(noDocumentsUpdated(id));
                }
            }
        } catch (MongoWriteException e) {
            info.rollbackVersion();
            throw e;
        }
        return entity;
    }

    /**
     * @return the Mapper used by this Datastore
     */
    public Mapper getMapper() {
        return mapper;
    }

    @Override
    public void shardCollections() {
        var entities = getMapper().getMappedEntities()
                                  .stream().filter(m -> m.getAnnotation(ShardKeys.class) != null)
                                  .collect(Collectors.toList());

        List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        entities.forEach(e -> {
            String name = e.getCollectionName();
            Document result;
            if (collectionNames.contains(name)) {
                result = withTransaction((session) -> {
                    return ((MorphiaSessionImpl) session).shardCollection(e);
                });
            } else {
                result = shardCollection(e);
            }
            if (!result.containsKey("collectionsharded")) {
                throw new MappingException(Sofia.cannotShardCollection(getDatabase().getName(), e.getCollectionName()));
            }
        });
    }

    @Override
    public <T> T merge(T entity) {
        return merge(entity, new InsertOneOptions());
    }

    protected Document shardCollection(EntityModel model) {
        ShardKeys shardKeys = model.getAnnotation(ShardKeys.class);
        if (shardKeys != null) {
            final Document collstats = database.runCommand(new Document("collstats", model.getCollectionName()));
            if (collstats.getBoolean("sharded", false)) {
                LOG.debug("MongoCollection already exists and is sharded already; doing nothing. " + collstats);
            } else {
                ShardOptions options = shardKeys.options();

                Document command = new Document("shardCollection", format("%s.%s", getDatabase().getName(), model.getCollectionName()))
                                       .append("unique", options.unique())
                                       .append("numInitialChunks", options.numInitialChunks())
                                       .append("presplitHashedZones", options.presplitHashedZones());
                if (collstats.get("collation") != null) {
                    command.append("collation", new Document("locale", "simple"));
                }
                command.append("key", stream(shardKeys.value())
                                          .map(k -> new Document(k.value(), k.type().queryForm()))
                                          .reduce(new Document(), (a, m) -> {
                                              a.putAll(m);
                                              return a;
                                          }));

                return operations.runCommand(command);
            }
        }
        return new Document();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> save(List<T> entities, InsertManyOptions options) {
        if (entities.isEmpty()) {
            return List.of();
        }

        Map<Class<?>, List<T>> grouped = new LinkedHashMap<>();
        List<T> list = new ArrayList<>();
        for (T entity : entities) {
            Class<?> type = entity.getClass();

            EntityModel model = getMapper().getEntityModel(type);
            if (getMapper().getId(entity) != null || model.getVersionProperty() != null) {
                list.add(entity);
            } else {
                grouped.computeIfAbsent(type, c -> new ArrayList<>())
                       .add(entity);
            }
        }

        String alternate = options.collection();
        if (grouped.size() > 1 && alternate != null) {
            Sofia.logInsertManyAlternateCollection();
        }

        for (Entry<Class<?>, List<T>> entry : grouped.entrySet()) {
            MongoCollection<T> collection = configureCollection(options, (MongoCollection<T>) getCollection(entry.getKey()));
            operations.insertMany(collection, entry.getValue(), options);
        }

        InsertOneOptions insertOneOptions = new InsertOneOptions()
                                                .bypassDocumentValidation(options.bypassDocumentValidation())
                                                .collection(alternate)
                                                .writeConcern(options.writeConcern());
        for (T entity : list) {
            save(entity, insertOneOptions);
        }
        return entities;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Query<T> queryByExample(T example) {
        return queryFactory.createQuery(this, (Class<T>) example.getClass(), toDocument(example));
    }

    @Override
    public <T> void refresh(T entity) {
        Codec<T> refreshCodec = getRefreshCodec(entity);

        MongoCollection<?> collection = getCollection(entity.getClass());
        PropertyModel idField = mapper.getEntityModel(entity.getClass())
                                      .getIdProperty();
        if (idField == null) {
            throw new MappingException(Sofia.idRequired(entity.getClass().getName()));
        }

        Document id = collection.find(new Document("_id", idField.getValue(entity)), Document.class)
                                .iterator()
                                .next();

        refreshCodec.decode(new DocumentReader(id), DecoderContext.builder().checkedDiscriminator(true).build());
    }

    @Override
    public MorphiaSessionImpl startSession() {
        return new MorphiaSessionImpl(this, mongoClient.startSession());
    }

    @Override
    public MorphiaSessionImpl startSession(ClientSessionOptions options) {
        return new MorphiaSessionImpl(this, mongoClient.startSession(options));
    }

    @Override
    public <T> T save(T entity, InsertOneOptions options) {
        save(getCollection(entity.getClass()), entity, options);
        return entity;
    }

    public DatastoreOperations operations() {
        return operations;
    }

    protected <T> T doTransaction(MorphiaSessionImpl morphiaSession, MorphiaTransaction<T> body) {
        try (morphiaSession) {
            return morphiaSession.getSession().withTransaction(() -> body.execute(morphiaSession));
        }
    }

    @Override
    public <T> T withTransaction(MorphiaTransaction<T> body) {
        return doTransaction(startSession(), body);
    }

    @Override
    public <T> T withTransaction(ClientSessionOptions options, MorphiaTransaction<T> transaction) {
        return doTransaction(startSession(options), transaction);
    }

    @Override
    public <T> List<T> replace(List<T> entities, ReplaceOptions options) {
        for (T entity : entities) {
            replace(entity, options);
        }

        return entities;
    }

    @Override
    public dev.morphia.aggregation.AggregationPipeline createAggregation(String collection, Class<?> clazz) {
        return new dev.morphia.aggregation.AggregationPipelineImpl(this, getDatabase().getCollection(collection), clazz);
    }

    @Override
    public <T> Query<T> createQuery(Class<T> type, Document q) {
        return queryFactory.createQuery(this, type, q);
    }

    @Override
    @SuppressWarnings("removal")
    public <T> Query<T> queryByExample(String collection, T ex) {
        return queryByExample(ex);
    }

    /**
     * @param model      internal
     * @param validation internal
     * @morphia.internal
     */
    public void enableValidation(EntityModel model, Validation validation) {
        String collectionName = model.getCollectionName();
        try {
            getDatabase().runCommand(new Document("collMod", collectionName)
                                         .append("validator", parse(validation.value()))
                                         .append("validationLevel", validation.level().getValue())
                                         .append("validationAction", validation.action().getValue()));
        } catch (MongoCommandException e) {
            if (e.getCode() == 26) {
                getDatabase().createCollection(collectionName,
                    new CreateCollectionOptions()
                        .validationOptions(new ValidationOptions()
                                               .validator(parse(validation.value()))
                                               .validationLevel(validation.level())
                                               .validationAction(validation.action())));
            } else {
                throw e;
            }
        }
    }

    protected DatastoreImpl operations(DatastoreOperations operations) {
        this.operations = operations;
        return this;
    }

    private <T> void save(MongoCollection collection, T entity, InsertOneOptions options) {
        collection = configureCollection(options, collection);

        EntityModel entityModel = mapper.getEntityModel(entity.getClass());
        PropertyModel idProperty = entityModel.getIdProperty();
        Object id = idProperty != null ? idProperty.getValue(entity) : null;
        VersionBumpInfo info = updateVersioning(entity);

        try {
            if (id == null || info.versioned() && info.newVersion() == 1) {
                operations.insertOne(collection, entity, options);
            } else {
                ReplaceOptions updateOptions = new ReplaceOptions()
                                                   .bypassDocumentValidation(options.bypassDocumentValidation())
                                                   .upsert(true);
                Document filter = new Document("_id", id);
                info.filter(filter);
                entityModel.getShardKeys().forEach((property) -> {
                    filter.put(property.getMappedName(), property.getValue(entity));
                });

                UpdateResult updateResult = operations.replaceOne(collection, entity, filter, updateOptions);

                if (info.versioned() && updateResult.getModifiedCount() != 1) {
                    info.rollbackVersion();
                    throw new VersionMismatchException(entity.getClass(), id);
                }
            }
        } catch (MongoWriteException e) {
            if (info.versioned()) {
                info.rollbackVersion();
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw e;
        }
    }

    /**
     * Enables any document validation defined on the class
     *
     * @param model the model to use
     * @morphia.internal
     */
    private void enableDocumentValidation(EntityModel model) {
        Validation validation = model.getAnnotation(Validation.class);
        String collectionName = model.getCollectionName();
        if (validation != null) {
            try {
                getDatabase().runCommand(new Document("collMod", collectionName)
                                             .append("validator", parse(validation.value()))
                                             .append("validationLevel", validation.level().getValue())
                                             .append("validationAction", validation.action().getValue()));
            } catch (MongoCommandException e) {
                if (e.getCode() == 26) {
                    database.createCollection(collectionName,
                        new CreateCollectionOptions()
                            .validationOptions(new ValidationOptions()
                                                   .validator(parse(validation.value()))
                                                   .validationLevel(validation.level())
                                                   .validationAction(validation.action())));
                } else {
                    throw e;
                }
            }
        }
    }

    private <T> Codec<T> getRefreshCodec(T entity) {
        for (MorphiaCodecProvider codecProvider : morphiaCodecProviders) {
            Codec<T> refreshCodec = codecProvider.getRefreshCodec(entity, codecRegistry);
            if (refreshCodec != null) {
                return refreshCodec;
            }
        }
        throw new IllegalStateException(Sofia.noRefreshCodec(entity.getClass().getName()));
    }

    @NotNull
    private <T> Map<Class<?>, List<T>> groupByType(List<T> entities, Predicate<EntityModel> special) {
        Map<Class<?>, List<T>> grouped = new LinkedHashMap<>();
        for (T entity : entities) {
            Class<?> type = entity.getClass();

            EntityModel model = getMapper().getEntityModel(type);
            if (special.test(model)) {
                grouped.computeIfAbsent(Void.class, c -> new ArrayList<>())
                       .add(entity);
            } else {
                grouped.computeIfAbsent(type, c -> new ArrayList<>())
                       .add(entity);
            }
        }
        return grouped;
    }

    private void importModels() {
        ServiceLoader<EntityModelImporter> importers = ServiceLoader.load(EntityModelImporter.class);
        for (EntityModelImporter importer : importers) {
            for (EntityModel model : importer.getModels(getMapper())) {
                mapper.register(model);
            }

            morphiaCodecProviders.add(importer.getCodecProvider(this));
        }
    }

    /**
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the Document
     * @morphia.internal
     * @since 2.3
     */
    @MorphiaInternal
    private Document toDocument(Object entity) {
        final Class<?> type = mapper.getEntityModel(entity.getClass()).getType();

        DocumentWriter writer = new DocumentWriter(mapper);
        ((Codec) codecRegistry.get(type)).encode(writer, entity, EncoderContext.builder().build());

        return writer.getDocument();
    }

    private <T> VersionBumpInfo updateVersioning(T entity) {
        final EntityModel entityModel = mapper.getEntityModel(entity.getClass());
        PropertyModel versionProperty = entityModel.getVersionProperty();
        PropertyModel idProperty = entityModel.getIdProperty();
        if (versionProperty != null) {
            Long value = (Long) versionProperty.getValue(entity);
            long updated = value == null ? 1 : value + 1;
            versionProperty.setValue(entity, updated);
            return new VersionBumpInfo(entity, idProperty, versionProperty, value, updated);
        }

        return new VersionBumpInfo();
    }

    private static class NoDeleteResult extends DeleteResult {
        @Override
        public boolean wasAcknowledged() {
            return false;
        }

        @Override
        public long getDeletedCount() {
            return 0;
        }
    }

    public abstract static class DatastoreOperations {
        public abstract <T> long countDocuments(MongoCollection<T> collection, Document query, CountOptions options);

        public abstract <T> DeleteResult deleteMany(MongoCollection<T> collection, Document queryDocument, DeleteOptions options);

        public abstract <T> DeleteResult deleteOne(MongoCollection<T> collection, Document queryDocument, DeleteOptions options);

        public abstract <E> FindIterable<E> find(MongoCollection<E> collection, Document query);

        @Nullable
        public abstract <T> T findOneAndDelete(MongoCollection<T> mongoCollection, Document queryDocument, FindAndDeleteOptions options);

        @Nullable
        public abstract <T> T findOneAndUpdate(MongoCollection<T> collection, Document toDocument, Document update, ModifyOptions options);

        public abstract <T> InsertManyResult insertMany(MongoCollection<T> collection, List<T> list, InsertManyOptions options);

        public abstract <T> InsertOneResult insertOne(MongoCollection<T> collection, T entity, InsertOneOptions options);

        public abstract <T> UpdateResult replaceOne(MongoCollection<T> collection, T entity, Document filter, ReplaceOptions options);

        public abstract Document runCommand(Document command);

        public abstract <T> UpdateResult updateMany(MongoCollection<T> collection, Document queryObject, Document updateOperations,
                                                    UpdateOptions options);

        public abstract <T> UpdateResult updateMany(MongoCollection<T> collection, Document queryObject, List<Document> updateOperations,
                                                    UpdateOptions options);

        public abstract <T> UpdateResult updateOne(MongoCollection<T> collection, Document queryObject, Document updateOperations,
                                                   UpdateOptions options);

        public abstract <T> UpdateResult updateOne(MongoCollection<T> collection, Document queryObject, List<Document> updateOperations,
                                                   UpdateOptions options);

    }

    private class CollectionOperations extends DatastoreOperations {
        @Override
        public <T> long countDocuments(MongoCollection<T> collection, Document query, CountOptions options) {
            return collection.countDocuments(query, options);
        }

        @Override
        public <T> DeleteResult deleteMany(MongoCollection<T> collection, Document queryDocument, DeleteOptions options) {
            return collection.deleteMany(queryDocument, options);
        }

        @Override
        public <T> DeleteResult deleteOne(MongoCollection<T> collection, Document queryDocument, DeleteOptions options) {
            return collection.deleteOne(queryDocument, options);
        }

        @Override
        public <E> FindIterable<E> find(MongoCollection<E> collection, Document query) {
            return collection.find(query);
        }

        @Override
        public <T> T findOneAndDelete(MongoCollection<T> mongoCollection, Document queryDocument, FindAndDeleteOptions options) {
            return mongoCollection.findOneAndDelete(queryDocument, options);
        }

        @Override
        public <T> T findOneAndUpdate(MongoCollection<T> collection, Document query, Document update, ModifyOptions options) {
            return collection.findOneAndUpdate(query, update, options);
        }

        @Override
        public <T> InsertManyResult insertMany(MongoCollection<T> collection, List<T> list, InsertManyOptions options) {
            return collection.insertMany(list, options.options());
        }

        @Override
        public <T> InsertOneResult insertOne(MongoCollection<T> collection, T entity, InsertOneOptions options) {
            return collection.insertOne(entity, options.options());
        }

        @Override
        public <T> UpdateResult replaceOne(MongoCollection<T> collection, T entity, Document filter, ReplaceOptions options) {
            return collection.replaceOne(filter, entity, options);
        }

        @Override
        public Document runCommand(Document command) {
            return mongoClient
                       .getDatabase("admin")
                       .runCommand(command);
        }

        @Override
        public <T> UpdateResult updateMany(MongoCollection<T> collection, Document queryObject, Document updateOperations,
                                           UpdateOptions options) {
            return collection.updateMany(queryObject, updateOperations, options);
        }

        @Override
        public <T> UpdateResult updateOne(MongoCollection<T> collection, Document queryObject, Document updateOperations,
                                          UpdateOptions options) {
            return collection.updateOne(queryObject, updateOperations, options);
        }

        @Override
        public <T> UpdateResult updateMany(MongoCollection<T> collection, Document queryObject, List<Document> updateOperations,
                                           UpdateOptions options) {
            return collection.updateMany(queryObject, updateOperations, options);
        }

        @Override
        public <T> UpdateResult updateOne(MongoCollection<T> collection, Document queryObject, List<Document> updateOperations,
                                          UpdateOptions options) {
            return collection.updateOne(queryObject, updateOperations, options);
        }
    }
}
