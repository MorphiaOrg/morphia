package dev.morphia;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ValidationOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.AggregationImpl;
import dev.morphia.aggregation.experimental.codecs.AggregationCodecProvider;
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.IndexHelper;
import dev.morphia.annotations.Validation;
import dev.morphia.experimental.MorphiaSession;
import dev.morphia.experimental.MorphiaSessionImpl;
import dev.morphia.internal.SessionConfigurable;
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
import dev.morphia.mapping.codec.pojo.experimental.EntityModelImporter;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.Update;
import dev.morphia.query.UpdateException;
import dev.morphia.query.ValidationException;
import dev.morphia.query.experimental.updates.UpdateOperators;
import dev.morphia.sofia.Sofia;
import dev.morphia.transactions.experimental.MorphiaTransaction;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.lang.String.format;
import static org.bson.Document.parse;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @morphia.internal
 */
@SuppressWarnings({"unchecked", "rawtypes", "removal"})
public class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);
    private final MongoClient mongoClient;
    private final Mapper mapper;
    private final QueryFactory queryFactory;
    private final CodecRegistry codecRegistry;
    private final List<MorphiaCodecProvider> morphiaCodecProviders = new ArrayList<>();
    private MongoDatabase database;

    protected DatastoreImpl(Mapper mapper, MongoClient mongoClient, String dbName) {
        this.database = mongoClient.getDatabase(dbName);
        this.mapper = mapper;
        this.mongoClient = mongoClient;
        this.queryFactory = mapper.getOptions().getQueryFactory();
        if (mapper.getOptions().isAutoImportModels()) {
            importModels();
        }

        morphiaCodecProviders.add(new MorphiaCodecProvider(this));

        CodecRegistry codecRegistry = database.getCodecRegistry();
        List<CodecProvider> providers = new ArrayList<>(List.of(new MorphiaTypesCodecProvider(this),
            new PrimitiveCodecRegistry(codecRegistry),
            new EnumCodecProvider(),
            new AggregationCodecProvider(this)));

        providers.addAll(morphiaCodecProviders);
        providers.add(codecRegistry);
        this.codecRegistry = fromProviders(providers);

        updateDatabaseWithRegistry();
    }

    /**
     * Copy constructor for a datastore
     *
     * @param datastore the datastore to clone
     * @morphia.internal
     * @since 2.0
     */
    public DatastoreImpl(DatastoreImpl datastore) {
        this.database = datastore.database;
        this.mongoClient = datastore.mongoClient;
        this.mapper = datastore.mapper;
        this.queryFactory = datastore.queryFactory;
        this.codecRegistry = datastore.codecRegistry;
    }

    @Override
    public Aggregation<Document> aggregate(String source) {
        return new AggregationImpl(this, getDatabase().getCollection(source));
    }

    @Override
    public <T> Aggregation<T> aggregate(Class<T> source) {
        return new AggregationImpl(this, getCollection(source));
    }

    @Override
    public dev.morphia.aggregation.AggregationPipeline createAggregation(Class source) {
        return new dev.morphia.aggregation.AggregationPipelineImpl(this, getCollection(source), source);
    }

    @Override
    public <T> DeleteResult delete(T entity) {
        return delete(entity, new DeleteOptions().writeConcern(mapper.getWriteConcern(entity.getClass())));
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
            throw new MappingException(format(Sofia.deleteWithClass(entity.getClass().getName())));
        }
        Object id = mapper.getId(entity);
        return id != null
               ? find(entity.getClass())
                   .filter(eq("_id", id))
                   .delete(options)
               : new NoDeleteResult();
    }

    @Override
    public void enableDocumentValidation() {
        for (EntityModel model : mapper.getMappedEntities()) {
            enableDocumentValidation(model);
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
                        if (dbResult.getBoolean("capped")) {
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

    @Override
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
    public <T> Query<T> find(String collection, Class<T> type) {
        return queryFactory.createQuery(this, collection, type);
    }

    @Override
    public <T> Query<T> find(String collection) {
        Class<T> type = mapper.getClassFromCollection(collection);
        return queryFactory.createQuery(this, type);
    }

    @Override
    @Nullable
    public ClientSession findSession(SessionConfigurable<?> configurable) {
        return configurable.clientSession() != null
               ? configurable.clientSession()
               : getSession();
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
        if (annotation != null && WriteConcern.valueOf(annotation.concern()) != null) {
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
                .find(new Document("command.comment", "logged query: " + options.getQueryLogId()),
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

    /**
     * @return the Mapper used by this Datastore
     */
    public Mapper getMapper() {
        return mapper;
    }

    @Override
    public <T> void insert(T entity) {
        insert(entity, new InsertOneOptions()
            .writeConcern(mapper.getWriteConcern(entity.getClass())));
    }

    @Override
    public <T> void insert(T entity, InsertOneOptions options) {
        insert(getCollection(entity.getClass()), entity, options);
    }

    @Override
    public <T> void insert(List<T> entities, InsertManyOptions options) {
        if (!entities.isEmpty()) {
            Class<?> type = entities.get(0).getClass();
            EntityModel model = mapper.getEntityModel(type);
            for (T entity : entities) {
                setInitialVersion(model, entity);
            }

            MongoCollection mongoCollection = options.prepare(getCollection(type));
            ClientSession session = options.clientSession();
            if (session == null) {
                mongoCollection.insertMany(entities, options.getOptions());
            } else {
                mongoCollection.insertMany(session, entities, options.getOptions());
            }
        }
    }

    @Override
    public <T> T merge(T entity) {
        return merge(entity, new InsertOneOptions());
    }

    @Override
    public <T> T merge(T entity, InsertOneOptions options) {
        final Object id = mapper.getId(entity);
        if (id == null) {
            throw new MappingException("Could not get id for " + entity.getClass().getName());
        }

        final EntityModel model = mapper.getEntityModel(entity.getClass());
        final PropertyModel versionProperty = model.getVersionProperty();
        Long oldVersion = null;
        long newVersion = -1;

        if (versionProperty != null) {
            oldVersion = (Long) versionProperty.getValue(entity);
            newVersion = oldVersion == null ? 1L : oldVersion + 1;
        }

        final Query<T> query = (Query<T>) find(entity.getClass()).filter(eq("_id", id));
        if (newVersion != -1) {
            updateVersion(entity, versionProperty, newVersion);
            query.filter(eq(versionProperty.getMappedName(), oldVersion));
        }

        Update<T> update;
        if (!options.unsetMissing()) {
            update = query.update(UpdateOperators.set(entity));
        } else {
            update = ((MergingEncoder<T>) new MergingEncoder(query,
                (MorphiaCodec) codecRegistry.get(entity.getClass())))
                .encode(entity);
        }
        UpdateResult execute = update
            .execute(new UpdateOptions()
                .clientSession(findSession(options))
                .writeConcern(options.writeConcern()));
        if (execute.getModifiedCount() != 1) {
            updateVersion(entity, versionProperty, oldVersion);
            if (versionProperty != null) {
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw new UpdateException("Nothing updated");
        }

        return (T) find(entity.getClass()).filter(eq("_id", id)).iterator(new FindOptions().limit(1)).next();
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
    @SuppressWarnings("unchecked")
    public <T> List<T> save(List<T> entities, InsertManyOptions options) {
        if (entities.isEmpty()) {
            return List.of();
        }

        Map<MongoCollection, List<T>> grouped = new LinkedHashMap<>();
        List<T> list = new ArrayList<>();
        for (T entity : entities) {
            Class<?> type = entity.getClass();

            EntityModel model = getMapper().getEntityModel(type);
            if (getMapper().getId(entity) != null || model.getVersionProperty() != null) {
                list.add(entity);
            } else {
                grouped.computeIfAbsent(getCollection(type), c -> new ArrayList<>())
                       .add(entity);
            }
        }

        for (Entry<MongoCollection, List<T>> entry : grouped.entrySet()) {
            MongoCollection<T> collection = entry.getKey();
            ClientSession clientSession = options.clientSession();
            if (clientSession == null) {
                collection.insertMany(entry.getValue(), options.getOptions());
            } else {
                collection.insertMany(clientSession, entry.getValue(), options.getOptions());
            }
        }

        InsertOneOptions insertOneOptions = new InsertOneOptions()
            .bypassDocumentValidation(options.getBypassDocumentValidation())
            .clientSession(findSession(options))
            .writeConcern(options.writeConcern());
        for (T entity : list) {
            save(entity, insertOneOptions);
        }
        return entities;
    }

    @Override
    public <T> T save(T entity) {
        return save(entity, new InsertOneOptions());
    }

    @Override
    public <T> T save(T entity, InsertOneOptions options) {
        save(getCollection(entity.getClass()), entity, options);
        return entity;
    }

    @Override
    public MorphiaSession startSession() {
        return new MorphiaSessionImpl(this, mongoClient.startSession());
    }

    @Override
    public MorphiaSession startSession(ClientSessionOptions options) {
        return new MorphiaSessionImpl(this, mongoClient.startSession(options));
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
        if (collectionName == null) {
            throw new MappingException(Sofia.notTopLevelType());
        }
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
     * Converts an entity (POJO) to a Document.  A special field will be added to keep track of the class type.
     *
     * @param entity The POJO
     * @return the Document
     * @morphia.internal
     * @since 2.3
     */
    public Document toDocument(Object entity) {
        final Class<?> type = mapper.getEntityModel(entity.getClass()).getType();

        DocumentWriter writer = new DocumentWriter(mapper);
        ((Codec) codecRegistry.get(type)).encode(writer, entity, EncoderContext.builder().build());

        return writer.getDocument();
    }

    protected <T> void insert(MongoCollection collection, T entity, InsertOneOptions options) {
        setInitialVersion(mapper.getEntityModel(entity.getClass()), entity);
        MongoCollection mongoCollection = mapper.enforceWriteConcern(collection, entity.getClass());
        ClientSession clientSession = findSession(options);
        if (clientSession == null) {
            mongoCollection.insertOne(entity, options.getOptions());
        } else {
            mongoCollection.insertOne(clientSession, entity, options.getOptions());
        }
    }

    private <T> T doTransaction(MorphiaSession morphiaSession, MorphiaTransaction<T> body) {
        try (morphiaSession) {
            ClientSession session = morphiaSession.getSession();
            if (session == null) {
                throw new IllegalStateException("No session could be found for the transaction.");
            }
            return session.withTransaction(() -> body.execute(morphiaSession));
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
        if (validation != null && collectionName != null) {
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

    private void importModels() {
        ServiceLoader<EntityModelImporter> importers = ServiceLoader.load(EntityModelImporter.class);
        for (EntityModelImporter importer : importers) {
            for (EntityModel model : importer.importModels()) {
                mapper.register(model);
            }

            morphiaCodecProviders.add(importer.getCodecProvider());
        }
    }

    private <T> void save(MongoCollection collection, T entity, InsertOneOptions options) {
        ClientSession clientSession = findSession(options);

        PropertyModel idField = mapper.findIdProperty(entity.getClass());
        Object id = idField.getValue(entity);

        final EntityModel model = mapper.getEntityModel(entity.getClass());
        final PropertyModel versionProperty = model.getVersionProperty();
        Long oldVersion = null;
        long newVersion = -1;

        if (versionProperty != null) {
            oldVersion = (Long) versionProperty.getValue(entity);
            newVersion = oldVersion == null ? 1L : oldVersion + 1;
        }

        Runnable operation;

        if (id == null || newVersion == 1) {
            operation = () -> {
                if (clientSession == null) {
                    options.prepare(collection).insertOne(entity, options.getOptions());
                } else {
                    options.prepare(collection).insertOne(clientSession, entity, options.getOptions());
                }
            };
        } else {
            ReplaceOptions updateOptions = new ReplaceOptions()
                .bypassDocumentValidation(options.getBypassDocumentValidation())
                .upsert(true);
            Document filter = new Document("_id", id);
            if (versionProperty != null) {
                filter.put(versionProperty.getMappedName(), oldVersion);
            }
            operation = () -> {
                UpdateResult updateResult;
                if (clientSession == null) {
                    updateResult = options.prepare(collection).replaceOne(filter, entity, updateOptions);
                } else {
                    updateResult = options.prepare(collection).replaceOne(clientSession, filter, entity, updateOptions);
                }
                if (versionProperty != null && updateResult.getModifiedCount() != 1) {
                    throw new VersionMismatchException(entity.getClass(), id);
                }
            };
        }

        try {
            updateVersion(entity, versionProperty, newVersion);
            operation.run();
        } catch (MongoWriteException e) {
            updateVersion(entity, versionProperty, oldVersion);
            if (versionProperty != null) {
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw e;
        }
    }

    private <T> void setInitialVersion(@Nullable EntityModel entityModel, T entity) {
        if (entityModel != null) {
            PropertyModel versionProperty = entityModel.getVersionProperty();
            if (versionProperty != null) {
                Object value = versionProperty.getValue(entity);
                if (value != null && !value.equals(0)) {
                    throw new ValidationException(Sofia.versionManuallySet());
                } else {
                    versionProperty.setValue(entity, 1L);
                }
            }
        }
    }

    private void updateDatabaseWithRegistry() {
        this.database = database.withCodecRegistry(codecRegistry);
    }

    private <T> void updateVersion(T entity, @Nullable PropertyModel versionProperty, @Nullable Long newVersion) {
        if (versionProperty != null) {
            versionProperty.setValue(entity, newVersion);
        }
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
}
