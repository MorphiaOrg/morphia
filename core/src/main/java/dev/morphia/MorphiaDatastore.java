package dev.morphia;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
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
import dev.morphia.annotations.ShardKeys;
import dev.morphia.annotations.ShardOptions;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.internal.IndexHelper;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.CollectionConfiguration;
import dev.morphia.internal.ReadConfigurable;
import dev.morphia.internal.WriteConfigurable;
import dev.morphia.mapping.EntityModelImporter;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.ShardKeyType;
import dev.morphia.mapping.codec.EnumCodecProvider;
import dev.morphia.mapping.codec.MorphiaCodecProvider;
import dev.morphia.mapping.codec.MorphiaExpressionCodecProvider;
import dev.morphia.mapping.codec.MorphiaFilterCodecProvider;
import dev.morphia.mapping.codec.MorphiaTypesCodecProvider;
import dev.morphia.mapping.codec.MorphiaUpdateOperatorCodecProvider;
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
import dev.morphia.query.UpdateException;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.sofia.Sofia;
import dev.morphia.transactions.MorphiaTransaction;
import dev.morphia.transactions.SessionDatastore;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @hidden
 */
@MorphiaInternal
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MorphiaDatastore implements Datastore {
    private static final Logger LOG = LoggerFactory.getLogger(Datastore.class);
    private final MongoClient mongoClient;
    private final Mapper mapper;
    private final QueryFactory queryFactory;
    private final CodecRegistry codecRegistry;
    public List<MorphiaCodecProvider> morphiaCodecProviders = new ArrayList<>();
    private MongoDatabase database;
    private DatastoreOperations operations;

    /**
     * @param client the mongo client
     * @param config the config
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaDatastore(MongoClient client, MorphiaConfig config) {
        this.mongoClient = client;
        this.mapper = new Mapper(config);
        this.queryFactory = mapper.getConfig().queryFactory();
        importModels();

        MongoDatabase clientDatabase = mongoClient.getDatabase(config.database());
        codecRegistry = buildRegistry(clientDatabase.getCodecRegistry());

        this.database = clientDatabase.withCodecRegistry(this.codecRegistry);
        operations = new CollectionOperations();

        config.packages().forEach(packageName -> {
            Sofia.logMappingPackage(packageName);
            mapper.map(packageName);
        });
        if (config.applyCaps()) {
            applyCaps();
        }
        if (config.applyIndexes()) {
            applyIndexes();
        }
        if (config.applyDocumentValidations()) {
            applyDocumentValidations();
        }
    }

    /**
     * Copy constructor for a datastore
     *
     * @param datastore the datastore to clone
     * @hidden
     * @morphia.internal
     * @since 2.0
     */
    public MorphiaDatastore(MorphiaDatastore datastore) {
        this.mongoClient = datastore.mongoClient;
        this.database = mongoClient.getDatabase(datastore.mapper.getConfig().database());
        this.mapper = datastore.mapper.copy();
        this.queryFactory = datastore.queryFactory;
        this.operations = datastore.operations;
        codecRegistry = buildRegistry(mongoClient.getDatabase(mapper.getConfig().database()).getCodecRegistry());
    }

    private CodecRegistry buildRegistry(CodecRegistry codecRegistry) {
        morphiaCodecProviders.add(new MorphiaCodecProvider(this));

        List<CodecProvider> providers = new ArrayList<>();
        mapper.getConfig().codecProvider().ifPresent(providers::add);

        providers.addAll(List.of(new MorphiaTypesCodecProvider(this),
                new PrimitiveCodecRegistry(codecRegistry),
                new EnumCodecProvider(),
                new MorphiaExpressionCodecProvider(this),
                new MorphiaFilterCodecProvider(this),
                new MorphiaUpdateOperatorCodecProvider(this),
                new AggregationCodecProvider(this)));

        providers.addAll(morphiaCodecProviders);
        providers.add(codecRegistry);
        providers.add(new GeoJsonCodecProvider());
        codecRegistry = fromProviders(providers);
        return codecRegistry;
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
            Sofia.logInsertManyAlternateCollection(alternate);
        }

        grouped.forEach((key, list) -> {
            List<VersionBumpInfo> infos = list.stream()
                    .map(this::updateVersioning)
                    .collect(Collectors.toList());

            try {
                MongoCollection<T> collection = configureCollection(options,
                        (MongoCollection<T>) getCollection(key));
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

    /**
     * Applies configuration options to the collection
     *
     * @param <T>        the collection type
     * @param options    the options to apply
     * @param collection the collection to configure
     * @return the configured collection
     * @hidden
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

    public void applyDocumentValidations() {
        for (EntityModel model : mapper.getMappedEntities()) {
            enableDocumentValidation(model);
        }
    }

    public void enableDocumentValidation() {
        Sofia.logConfiguredOperation("Datastore#enableDocumentValidation()");
        for (EntityModel model : mapper.getMappedEntities()) {
            enableDocumentValidation(model);
        }
    }

    public void applyIndexes() {
        if (mapper.getMappedEntities().isEmpty()) {
            LOG.warn(Sofia.noMappedClasses());
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
        if (model.getIdProperty() != null) {
            indexHelper.createIndex(getCollection(type), model);
        }
    }

    @Override
    public <T> Query<T> find(Class<T> type) {
        return queryFactory.createQuery(this, type);
    }

    @Override
    public <T> Query<T> find(Class<T> type, Document nativeQuery) {
        return queryFactory.createQuery(this, type, nativeQuery);
    }

    /**
     * @param collection the collection
     * @param type       the type Class
     * @param <T>        the query type
     * @return the new query
     */
    public <T> Query<T> find(String collection, Class<T> type) {
        return queryFactory.createQuery(this, collection, type);
    }

    /**
     * @param collection the collection
     * @param <T>        the query type
     * @return the new query
     */
    public <T> Query<T> find(String collection) {
        Class<T> type = mapper.getClassFromCollection(collection);
        return queryFactory.createQuery(this, type);
    }

    /**
     * @return the codec registry
     */
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    @Override
    public <T> MongoCollection<T> getCollection(Class<T> type) {
        EntityModel entityModel = mapper.getEntityModel(type);
        String collectionName = entityModel.collectionName();

        MongoCollection<T> collection = getDatabase().getCollection(collectionName, type)
                .withCodecRegistry(codecRegistry);

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

    @Override
    public <T> T replace(T entity, ReplaceOptions options) {
        MongoCollection collection = configureCollection(options, getCollection(entity.getClass()));

        EntityModel entityModel = mapper.getEntityModel(entity.getClass());
        PropertyModel idProperty = entityModel.getIdProperty();
        Object id = idProperty != null ? idProperty.getValue(entity) : null;
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

    private void applyCaps() {
        List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        for (EntityModel model : mapper.getMappedEntities()) {
            Entity entityAnnotation = model.getEntityAnnotation();
            if (entityAnnotation != null) {
                CappedAt cappedAt = entityAnnotation.cap();
                if (cappedAt.value() > 0 || cappedAt.count() > 0) {
                    final CappedAt cap = entityAnnotation.cap();
                    final String collName = model.collectionName();
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

        final Query<T> query = info.filter((Query<T>) find(entity.getClass()).filter(eq("_id", id)));

        UpdateResult execute;
        UpdateOptions updateOptions = new UpdateOptions()
                .writeConcern(options.writeConcern());
        if (!options.unsetMissing()) {
            execute = query.update(updateOptions, set(entity));
        } else {
            MorphiaCodec morphiaCodec = (MorphiaCodec) codecRegistry.get(entity.getClass());
            var updates = ((MergingEncoder<T>) new MergingEncoder(query, morphiaCodec, mapper.getConfig()))
                    .encode(entity);
            execute = query.update(updateOptions, updates.toArray(new UpdateOperator[0]));
        }
        if (execute.getMatchedCount() != 1) {
            if (info.versioned()) {
                info.rollbackVersion();
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw new UpdateException(Sofia.noMatchingDocuments());
        }

        return (T) find(entity.getClass()).filter(eq("_id", id)).iterator(new FindOptions().limit(1)).next();
    }

    protected MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * @return the Mapper used by this Datastore
     */
    public Mapper getMapper() {
        return mapper;
    }

    public void shardCollections() {
        var entities = getMapper().getMappedEntities()
                .stream().filter(m -> m.getAnnotation(ShardKeys.class) != null)
                .collect(Collectors.toList());

        operations.runCommand(new Document("enableSharding", database.getName()));

        entities.forEach(e -> {
            if (!shardCollection(e).containsKey("collectionsharded")) {
                throw new MappingException(Sofia.cannotShardCollection(getDatabase().getName(), e.collectionName()));
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
            final Document collstats = database.runCommand(new Document("collstats", model.collectionName()));
            if (collstats.getBoolean("sharded", false)) {
                LOG.debug("MongoCollection already exists and is sharded already; doing nothing. " + collstats);
            } else {
                ShardOptions options = shardKeys.options();

                Document command = new Document("shardCollection", format("%s.%s", getDatabase().getName(), model.collectionName()))
                        .append("unique", options.unique())
                        .append("presplitHashedZones", options.presplitHashedZones());
                var hashed = stream(shardKeys.value()).anyMatch(k -> k.type() == ShardKeyType.HASHED);
                if (hashed) {
                    if (options.numInitialChunks() != -1) {
                        command.append("numInitialChunks", options.numInitialChunks());
                    }
                }

                if (collstats.get("collation") != null) {
                    command.append("collation", new Document("locale", "simple"));
                }
                command.append("key", stream(shardKeys.value())
                        .map(k -> new Document(k.value(), queryForm(k.type())))
                        .reduce(new Document(), (a, m) -> {
                            a.putAll(m);
                            return a;
                        }));

                return operations.runCommand(command);
            }
        }
        return new Document();
    }

    private Object queryForm(ShardKeyType type) {
        switch (type) {
            case HASHED:
                return "hashed";
            case RANGED:
                return 1;
        }

        throw new IllegalStateException("Every shard key type should be handled.");
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
            Sofia.logInsertManyAlternateCollection(alternate);
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
    public SessionDatastore startSession() {
        return new SessionDatastore(this, mongoClient.startSession());
    }

    @Override
    public SessionDatastore startSession(ClientSessionOptions options) {
        return new SessionDatastore(this, mongoClient.startSession(options));
    }

    @Override
    public <T> T save(T entity, InsertOneOptions options) {
        save(getCollection(entity.getClass()), entity, options);
        return entity;
    }

    /**
     * @return the operations
     */
    public DatastoreOperations operations() {
        return operations;
    }

    /**
     * @param morphiaSession the session
     * @param body           the transaction body
     * @param <T>            the type of the result
     * @return the result
     */
    @Nullable
    protected <T> T doTransaction(SessionDatastore morphiaSession, MorphiaTransaction<T> body) {
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

    /**
     * @param model      internal
     * @param validation internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public void enableValidation(EntityModel model, Validation validation) {
        String collectionName = model.collectionName();
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

    /**
     * @param operations the operations
     * @return this
     */
    protected MorphiaDatastore operations(DatastoreOperations operations) {
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
                        .upsert(!info.versioned);
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
            }
            throw e;
        }
    }

    /**
     * Enables any document validation defined on the class
     *
     * @param model the model to use
     */
    private void enableDocumentValidation(EntityModel model) {
        Validation validation = model.getAnnotation(Validation.class);
        String collectionName = model.collectionName();
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

    @NonNull
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

    public boolean hasLifecycle(EntityModel model, Class<? extends Annotation> type) {
        return model.hasLifecycle(type)
                || mapper.getListeners().stream()
                        .anyMatch(listener -> listener.hasAnnotation(type));
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
     * Converts an entity (POJO) to a Document. A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the Document
     * @since 2.3
     */
    private Document toDocument(Object entity) {
        return DocumentWriter.encode(entity, this.getMapper(), this.getCodecRegistry());
    }

    private <T> VersionBumpInfo updateVersioning(T entity) {
        final EntityModel entityModel = mapper.getEntityModel(entity.getClass());
        PropertyModel versionProperty = entityModel.getVersionProperty();
        if (versionProperty != null) {
            Long value = (Long) versionProperty.getValue(entity);
            long updated = value == null ? 1 : value + 1;
            versionProperty.setValue(entity, updated);
            return new VersionBumpInfo(entity, versionProperty, value, updated);
        }

        return new VersionBumpInfo(entity);
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

    /**
     * Defines the various operations the driver performs on behalf of a Datastore
     */
    public abstract static class DatastoreOperations {
        /**
         * Counts the number of documents in the collection according to the given options.
         * 
         * @param collection the collection to use
         * @param query      the query to use
         * @param options    the options to apply
         * @return the count of documents found
         * @param <T> the entity type
         */
        public abstract <T> long countDocuments(MongoCollection<T> collection, Document query, CountOptions options);

        /**
         * Removes all documents from the collection that match the given query filter. If no documents match, the collection is not
         * modified.
         * 
         * @param collection the collection to use
         * @param query      the query to use
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> DeleteResult deleteMany(MongoCollection<T> collection, Document query, DeleteOptions options);

        /**
         * Removes one document from the collection that match the given query filter. If no documents match, the collection is not
         * modified.
         * 
         * @param collection the collection to use
         * @param query      the query to use
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> DeleteResult deleteOne(MongoCollection<T> collection, Document query, DeleteOptions options);

        /**
         * Finds all documents in the collection.
         *
         * @param collection the collection to use
         * @param query      the query to use
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> FindIterable<T> find(MongoCollection<T> collection, Document query);

        /**
         * Atomically find a document and remove it.
         *
         * @param collection the collection to use
         * @param query      the query to use
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        @Nullable
        public abstract <T> T findOneAndDelete(MongoCollection<T> collection, Document query, FindAndDeleteOptions options);

        /**
         * Atomically find a document and update it.
         *
         * @param collection the collection to use
         * @param query      the query to use
         * @param update     the update to apply
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        @Nullable
        public abstract <T> T findOneAndUpdate(MongoCollection<T> collection, Document query, Document update, ModifyOptions options);

        /**
         * Inserts one or more documents.
         *
         * @param collection the collection to use
         * @param list       the entities to insert
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> InsertManyResult insertMany(MongoCollection<T> collection, List<T> list, InsertManyOptions options);

        /**
         * Inserts one document.
         *
         * @param collection the collection to use
         * @param entity     the entity to insert
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> InsertOneResult insertOne(MongoCollection<T> collection, T entity, InsertOneOptions options);

        /**
         * Replaces one document.
         *
         * @param collection the collection to use
         * @param entity     the entity to replace
         * @param filter     the filter to use
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> UpdateResult replaceOne(MongoCollection<T> collection, T entity, Document filter, ReplaceOptions options);

        /**
         * Runs a command on the server
         * 
         * @param command the command
         * @return the results
         */
        public abstract Document runCommand(Document command);

        /**
         * Updates one or more documents.
         *
         * @param collection the collection to use
         * @param query      the entity to replace
         * @param updates    the updates to apply
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> UpdateResult updateMany(MongoCollection<T> collection, Document query, Document updates, UpdateOptions options);

        /**
         * Updates one or more documents.
         *
         * @param collection the collection to use
         * @param query      the entity to replace
         * @param updates    the updates to apply
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> UpdateResult updateMany(MongoCollection<T> collection, Document query, List<Document> updates,
                UpdateOptions options);

        /**
         * Updates one document.
         *
         * @param collection the collection to use
         * @param query      the entity to replace
         * @param updates    the updates to apply
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> UpdateResult updateOne(MongoCollection<T> collection, Document query, Document updates, UpdateOptions options);

        /**
         * Updates one document.
         *
         * @param collection the collection to use
         * @param query      the entity to replace
         * @param updates    the updates to apply
         * @param options    the options to apply
         * @return the results
         * @param <T> the entity type
         */
        public abstract <T> UpdateResult updateOne(MongoCollection<T> collection, Document query, List<Document> updates,
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
            return collection.insertMany(list, options.driver());
        }

        @Override
        public <T> InsertOneResult insertOne(MongoCollection<T> collection, T entity, InsertOneOptions options) {
            return collection.insertOne(entity, options.driver());
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
        public <T> UpdateResult updateMany(MongoCollection<T> collection, Document query, Document updates,
                UpdateOptions options) {
            return collection.updateMany(query, updates, options);
        }

        @Override
        public <T> UpdateResult updateOne(MongoCollection<T> collection, Document query, Document updates,
                UpdateOptions options) {
            try {
                return collection.updateOne(query, updates, options);
            } catch (MongoWriteException e) {
                throw e;
            }
        }

        @Override
        public <T> UpdateResult updateMany(MongoCollection<T> collection, Document query, List<Document> updates,
                UpdateOptions options) {
            return collection.updateMany(query, updates, options);
        }

        @Override
        public <T> UpdateResult updateOne(MongoCollection<T> collection, Document query, List<Document> updates,
                UpdateOptions options) {
            return collection.updateOne(query, updates, options);
        }
    }

    @MorphiaInternal
    private static class VersionBumpInfo {
        private final Long oldVersion;
        private final boolean versioned;
        private final Long newVersion;
        private final PropertyModel versionProperty;
        private final Object entity;

        <T> VersionBumpInfo(T entity) {
            versioned = false;
            newVersion = null;
            oldVersion = null;
            versionProperty = null;
            this.entity = entity;
        }

        <T> VersionBumpInfo(T entity, PropertyModel versionProperty, @Nullable Long oldVersion, Long newVersion) {
            this.entity = entity;
            versioned = true;
            this.newVersion = newVersion;
            this.oldVersion = oldVersion;
            this.versionProperty = versionProperty;
        }

        public void filter(Document filter) {
            if (versionProperty != null) {
                filter.put(versionProperty.getMappedName(), oldVersion());
            }
        }

        public <T> Query<T> filter(Query<T> query) {
            if (versionProperty != null && newVersion() != -1) {
                query.filter(eq(versionProperty.getMappedName(), oldVersion()));
            }

            return query;
        }

        public Long newVersion() {
            return newVersion;
        }

        public Long oldVersion() {
            return oldVersion;
        }

        public void rollbackVersion() {
            if (versionProperty != null) {
                versionProperty.setValue(entity, oldVersion);
            }
        }

        public boolean versioned() {
            return versioned;
        }
    }
}
