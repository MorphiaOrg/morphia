package dev.morphia;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
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
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.builders.IndexHelper;
import dev.morphia.experimental.MorphiaSession;
import dev.morphia.experimental.MorphiaSessionImpl;
import dev.morphia.internal.SessionConfigurable;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.MergingEncoder;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.Update;
import dev.morphia.query.UpdateException;
import dev.morphia.query.experimental.updates.UpdateOperators;
import dev.morphia.sofia.Sofia;
import dev.morphia.transactions.experimental.MorphiaTransaction;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.bson.Document.parse;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @morphia.internal
 */
@SuppressWarnings({"unchecked", "rawtypes", "removal"})
public class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);

    private final MongoDatabase database;
    private final MongoClient mongoClient;
    private final Mapper mapper;

    private final QueryFactory queryFactory;

    protected DatastoreImpl(MongoClient mongoClient, MapperOptions options, String dbName) {
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.mapper = new Mapper(this, database.getCodecRegistry(), options);
        this.database = database.withCodecRegistry(mapper.getCodecRegistry());
        this.mongoClient = mongoClient;

        this.queryFactory = options.getQueryFactory();
    }

    /**
     * Copy constructor for a datastore
     *
     * @param database     the database
     * @param mongoClient  the client
     * @param mapper       the mapper
     * @param queryFactory the query factory
     * @morphia.internal
     * @since 2.0
     */
    public DatastoreImpl(MongoDatabase database, MongoClient mongoClient, Mapper mapper,
                         QueryFactory queryFactory) {
        this.database = database;
        this.mongoClient = mongoClient;
        this.mapper = mapper;
        this.queryFactory = queryFactory;
    }

    @Override
    public Aggregation<Document> aggregate(String source) {
        return new AggregationImpl(this, getDatabase().getCollection(source));
    }

    @Override
    public <T> Aggregation<T> aggregate(Class<T> source) {
        return new AggregationImpl(this, mapper.getCollection(source));
    }

    @Override
    public dev.morphia.aggregation.AggregationPipeline createAggregation(Class source) {
        return new dev.morphia.aggregation.AggregationPipelineImpl(this, mapper.getCollection(source), source);
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
                indexHelper.createIndex(mapper.getCollection(model.getType()), model);
            }
        }
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
    public <T> Query<T> find(Class<T> type) {
        return queryFactory.createQuery(this, type);
    }

    @Override
    public <T> Query<T> find(String collection, Class<T> type) {
        return queryFactory.createQuery(this, collection, type);
    }

    @Override
    @Nullable
    public ClientSession findSession(SessionConfigurable<?> configurable) {
        return configurable.clientSession() != null
               ? configurable.clientSession()
               : getSession();
    }

    @Override
    @SuppressWarnings("removal")
    public <T> Query<T> queryByExample(String collection, T ex) {
        return queryByExample(ex);
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
                    json = filter.toJson(mapper.getCodecRegistry().get(Document.class));
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
    public <T> void ensureIndexes(Class<T> type) {
        EntityModel model = mapper.getEntityModel(type);
        final IndexHelper indexHelper = new IndexHelper(mapper);
        indexHelper.createIndex(mapper.getCollection(type), model);
    }

    @Override
    public <T> Query<T> find(String collection) {
        Class<T> type = mapper.getClassFromCollection(collection);
        return queryFactory.createQuery(this, type);
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }

    @Override
    public <T> void insert(T entity) {
        insert(entity, new InsertOneOptions()
            .writeConcern(mapper.getWriteConcern(entity.getClass())));
    }

    @Override
    public <T> void insert(T entity, InsertOneOptions options) {
        save(entity, options);
    }

    @Override
    public <T> void insert(List<T> entities, InsertManyOptions options) {
        save(entities, options);
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

        VersionBumpInfo info = updateVersioning(entity);

        final Query<T> query = (Query<T>) find(entity.getClass()).filter(eq("_id", id));
        if (info.versioned && info.newVersion != -1) {
            query.filter(eq(info.versionProperty.getMappedName(), info.oldVersion));
        }

        Update<T> update;
        if (!options.unsetMissing()) {
            update = query.update(UpdateOperators.set(entity));
        } else {
            update = ((MergingEncoder<T>) new MergingEncoder(query,
                (MorphiaCodec) mapper.getCodecRegistry().get(entity.getClass())))
                .encode(entity);
        }
        UpdateResult execute = update
            .execute(new UpdateOptions()
                .clientSession(findSession(options))
                .writeConcern(options.writeConcern()));
        if (execute.getModifiedCount() != 1) {
            if (info.versioned) {
                info.rollbackVersion(entity);
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw new UpdateException("Nothing updated");
        }

        return (T) find(entity.getClass()).filter(eq("_id", id)).iterator(new FindOptions().limit(1)).next();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Query<T> queryByExample(T example) {
        return queryFactory.createQuery(this, (Class<T>) example.getClass(), mapper.toDocument(example));
    }

    @Override
    public <T> void refresh(T entity) {
        getMapper().refresh(entity);
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
                grouped.computeIfAbsent(mapper.getCollection(type), c -> new ArrayList<>())
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
        return save(entity, new InsertOneOptions()
            .writeConcern(mapper.getWriteConcern(entity.getClass())));
    }

    @Override
    public <T> T save(T entity, InsertOneOptions options) {
        save(mapper.getCollection(entity.getClass()), entity, options);
        return entity;
    }

    @Override
    public MorphiaSession startSession() {
        return new MorphiaSessionImpl(mongoClient.startSession(), mongoClient, database, mapper, queryFactory);
    }

    @Override
    public MorphiaSession startSession(ClientSessionOptions options) {
        return new MorphiaSessionImpl(mongoClient.startSession(options), mongoClient, database, mapper, queryFactory);
    }

    @Override
    public <T> T withTransaction(MorphiaTransaction<T> body) {
        return doTransaction(startSession(), body);
    }

    @Override
    public <T> T withTransaction(ClientSessionOptions options, MorphiaTransaction<T> transaction) {
        return doTransaction(startSession(options), transaction);
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

    private <T> void save(MongoCollection collection, T entity, InsertOneOptions options) {
        ClientSession clientSession = findSession(options);

        Object id = mapper.findIdProperty(entity.getClass()).getValue(entity);
        VersionBumpInfo info = updateVersioning(entity);

        try {
            if (id == null || info.versioned && info.newVersion == 1) {
                if (clientSession == null) {
                    options.prepare(collection).insertOne(entity, options.getOptions());
                } else {
                    options.prepare(collection).insertOne(clientSession, entity, options.getOptions());
                }
            } else {
                ReplaceOptions updateOptions = new ReplaceOptions()
                    .bypassDocumentValidation(options.getBypassDocumentValidation())
                    .upsert(true);
                Document filter = new Document("_id", id);
                if (info.versioned) {
                    filter.put(info.versionProperty.getMappedName(), info.oldVersion);
                }
                UpdateResult updateResult = clientSession == null
                                            ? options.prepare(collection).replaceOne(filter, entity, updateOptions)
                                            : options.prepare(collection).replaceOne(clientSession, filter, entity, updateOptions);

                if (info.versioned && updateResult.getModifiedCount() != 1) {
                    info.rollbackVersion(entity);
                    throw new VersionMismatchException(entity.getClass(), id);
                }
            }
        } catch (MongoWriteException e) {
            if (info.versioned) {
                info.rollbackVersion(entity);
                throw new VersionMismatchException(entity.getClass(), id);
            }
            throw e;
        }
    }

    private <T> VersionBumpInfo updateVersioning(T entity) {
        final EntityModel entityModel = mapper.getEntityModel(entity.getClass());
        PropertyModel versionProperty = entityModel.getVersionProperty();
        if (versionProperty != null) {
            Long value = (Long) versionProperty.getValue(entity);
            long updated = value == null ? 1 : value + 1;
            versionProperty.setValue(entity, updated);
            return new VersionBumpInfo(versionProperty, value, updated);
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

    private static class VersionBumpInfo {
        private final Long oldVersion;
        private final boolean versioned;
        private final Long newVersion;
        private final PropertyModel versionProperty;

        private VersionBumpInfo() {
            versioned = false;
            newVersion = null;
            oldVersion = null;
            versionProperty = null;
        }

        private VersionBumpInfo(PropertyModel versionProperty, @Nullable Long oldVersion, Long newVersion) {
            versioned = true;
            this.newVersion = newVersion;
            this.oldVersion = oldVersion;
            this.versionProperty = versionProperty;
        }

        private <T> void rollbackVersion(T entity) {
            if (versioned) {
                versionProperty.setValue(entity, oldVersion);
            }
        }
    }
}
