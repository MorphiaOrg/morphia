package dev.morphia;

import com.mongodb.ClientSessionOptions;
import com.mongodb.DBRef;
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
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.aggregation.AggregationPipelineImpl;
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Validation;
import dev.morphia.experimental.MorphiaSession;
import dev.morphia.experimental.MorphiaSessionImpl;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MappingException;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.UpdateException;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateOpsImpl;
import dev.morphia.query.ValidationException;
import dev.morphia.sofia.Sofia;
import dev.morphia.transactions.experimental.MorphiaTransaction;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.bson.Document.parse;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);

    private final MongoDatabase database;
    private final IndexHelper indexHelper;
    private final MongoClient mongoClient;
    private Mapper mapper;

    private volatile QueryFactory queryFactory = new DefaultQueryFactory();

    protected DatastoreImpl(final MongoClient mongoClient, final MapperOptions options, final String dbName) {
        this.mongoClient = mongoClient;
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.mapper = new Mapper(this, database.getCodecRegistry(), options);

        this.database = database.withCodecRegistry(mapper.getCodecRegistry());
        this.indexHelper = new IndexHelper(mapper, this.database);
    }

    public DatastoreImpl(final MongoDatabase database,
                         final IndexHelper indexHelper,
                         final MongoClient mongoClient,
                         final Mapper mapper,
                         final QueryFactory queryFactory) {
        this.database = database;
        this.indexHelper = indexHelper;
        this.mongoClient = mongoClient;
        this.mapper = mapper;
        this.queryFactory = queryFactory;
    }

    @Override
    public <T> T withTransaction(final MorphiaTransaction<T> body) {
        return doTransaction(mongoClient.startSession(), body);
    }

    @Override
    public <T> T  withTransaction(final MorphiaTransaction<T> transaction, final ClientSessionOptions options) {
        return doTransaction(mongoClient.startSession(options), transaction);
    }

    private <T> T doTransaction(final ClientSession session, final MorphiaTransaction<T> body) {
        return session.withTransaction(() -> {
            try(MorphiaSession morphiaSession = new MorphiaSessionImpl(session, mongoClient, database, mapper, indexHelper, queryFactory)) {
                return body.execute(morphiaSession);
            }
        });
    }

    /**
     * @param source the initial type/collection to aggregate against
     * @return a new query bound to the kind (a specific {@link MongoCollection})
     */
    @Override
    public AggregationPipeline createAggregation(final Class source) {
        return new AggregationPipelineImpl(this, getCollection(source), source);
    }

    @Override
    public <T> Query<T> find(final Class<T> clazz) {
        return newQuery(clazz);
    }

    /**
     * @return the Mapper used by this Datastore
     */
    public Mapper getMapper() {
        return mapper;
    }

    @Override
    public <T> DeleteResult delete(final T entity) {
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
    public <T> DeleteResult delete(final T entity, final DeleteOptions options) {
        if (entity instanceof Class<?>) {
            throw new MappingException("Did you mean to delete all documents? -- delete(ds.createQuery(???.class))");
        }
        return find(entity.getClass()).filter("_id", mapper.getId(entity)).remove(options);
    }

    @Override
    public void ensureCaps() {
        List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        for (final MappedClass mc : mapper.getMappedClasses()) {
            if (mc.getEntityAnnotation() != null && mc.getEntityAnnotation().cap().value() > 0) {
                final CappedAt cap = mc.getEntityAnnotation().cap();
                final String collName = mc.getCollectionName();
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

    @Override
    public void enableDocumentValidation() {
        for (final MappedClass mc : mapper.getMappedClasses()) {
            process(mc, mc.getAnnotation(Validation.class));
        }
    }

    @Override
    public void ensureIndexes() {
        if (mapper.getMappedClasses().isEmpty()) {
            Sofia.logNoMappedClasses();
        }
        for (final MappedClass mc : mapper.getMappedClasses()) {
            if (mc.getEntityAnnotation() != null) {
                indexHelper.createIndex(getCollection(mc.getType()), mc);
            }
        }
    }

    @Override
    public <T> void ensureIndexes(final Class<T> clazz) {
        indexHelper.createIndex(getCollection(clazz), mapper.getMappedClass(clazz));
    }

    @Override
    public <T, V> Query<T> get(final Class<T> clazz, final Iterable<V> ids) {
        return find(clazz).disableValidation().filter("_id" + " in", ids).enableValidation();
    }

    @Override
    public <T> T getByKey(final Class<T> clazz, final Key<T> key) {
        final String collectionName = mapper.getMappedClass(clazz).getCollectionName();
        final String keyCollection = mapper.updateCollection(key);
        if (!collectionName.equals(keyCollection)) {
            throw new RuntimeException("collection names don't match for key and class: " + collectionName + " != " + keyCollection);
        }

        Object id = key.getId();
        if (id instanceof Document) {
            ((Document) id).remove(mapper.getOptions().getDiscriminatorKey());
        }
        return find(clazz).filter("_id", id)
                          .first(new FindOptions().limit(1));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> List<T> getByKeys(final Class<T> clazz, final Iterable<Key<T>> keys) {

        final Map<String, List<Key>> kindMap = new HashMap<>();
        final List<T> entities = new ArrayList<>();
        for (final Key<?> key : keys) {
            mapper.updateCollection(key);

            if (kindMap.containsKey(key.getCollection())) {
                kindMap.get(key.getCollection()).add(key);
            } else {
                kindMap.put(key.getCollection(), new ArrayList<>(singletonList((Key) key)));
            }
        }
        for (final Map.Entry<String, List<Key>> entry : kindMap.entrySet()) {
            final List<Key> kindKeys = entry.getValue();

            final List<Object> objIds = new ArrayList<>();
            for (final Key key : kindKeys) {
                objIds.add(key.getId());
            }
            final List kindResults = find(entry.getKey()).disableValidation().filter("_id in", objIds)
                                                         .execute()
                                                         .toList();
            entities.addAll(kindResults);
        }

        // TODO: order them based on the incoming Keys.
        return entities;
    }

    @Override
    public <T> List<T> getByKeys(final Iterable<Key<T>> keys) {
        return getByKeys(null, keys);
    }

    public <T> MongoCollection<T> getCollection(final Class<T> type) {
        MappedClass mappedClass = mapper.getMappedClass(type);
        if (mappedClass == null) {
            throw new MappingException(Sofia.notMappable(type.getName()));
        }
        if (mappedClass.getCollectionName() == null) {
            throw new MappingException(Sofia.noMappedCollection(type.getName()));
        }

        MongoCollection<T> collection = null;
        if (mappedClass.getEntityAnnotation() != null) {
            collection = getDatabase().getCollection(mappedClass.getCollectionName(), type);
            collection = enforceWriteConcern(collection, type, null);
        }
        return collection;
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }

    @Override
    @Deprecated
    // use mapper instead.
    public <T> Key<T> getKey(final T entity) {
        return mapper.getKey(entity);
    }

    @Override
    public QueryFactory getQueryFactory() {
        return queryFactory;
    }

    @Override
    public void setQueryFactory(final QueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public <T> T merge(final T entity) {
        return merge(entity, new InsertOneOptions());
    }

    @Override
    public <T> T merge(final T entity, final InsertOneOptions options) {
        final Object id = mapper.getId(entity);
        if (id == null) {
            throw new MappingException("Could not get id for " + entity.getClass().getName());
        }

        final Document document = mapper.toDocument(entity);
        document.remove("_id");

        final MappedClass mc = mapper.getMappedClass(entity.getClass());
        final MongoCollection collection = getCollection(entity.getClass());

        final Query<T> query = (Query<T>) find(entity.getClass()).filter("_id", id);
        if (!tryVersionedUpdate(collection, entity, options, mc)) {
            UpdateResult execute = query.update()
                                        .set(entity)
                                        .execute(new UpdateOptions().writeConcern(options.writeConcern()));
            if (execute.getModifiedCount() != 1) {
                throw new UpdateException("Nothing updated");
            }
        }

        return query.first();
    }

    @Override
    public <T> void merge(final T entity, final WriteConcern wc) {
        merge(entity, new InsertOneOptions().writeConcern(wc));
    }

    @SuppressWarnings("unchecked")
    public <T> Query<T> queryByExample(final T example) {
        final Class<T> type = (Class<T>) example.getClass();
        return newQuery(type, mapper.toDocument(example));
    }

    @Override
    public <T> List<T> save(final List<T> entities) {
        return save(entities, new InsertManyOptions());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> save(final List<T> entities, final InsertManyOptions options) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Class<?> first = entities.get(0).getClass();
        boolean allMatch = entities.stream().map(e -> e.getClass()).allMatch(c -> c.equals(first));

        if (allMatch) {
            MongoCollection<T> collection = (MongoCollection<T>) getCollection(entities.get(0).getClass());
            collection.insertMany(entities, options.getOptions());
        } else {
            InsertOneOptions insertOneOptions = new InsertOneOptions()
                                                    .bypassDocumentValidation(options.getBypassDocumentValidation())
                                                    .writeConcern(options.getWriteConcern());
            for (final T entity : entities) {
                save(entity, insertOneOptions);
            }
        }
        return entities;
    }

    @Override
    public <T> T save(final T entity) {
        save(entity, new InsertOneOptions());
        return entity;
    }

    @Override
    public <T> T save(final T entity, final InsertOneOptions options) {
        if (entity == null) {
            throw new UpdateException("Can not persist a null entity");
        }

        save(getCollection(entity.getClass()), entity, options);
        return entity;
    }

    public MongoCollection enforceWriteConcern(final MongoCollection collection, final Class type, final WriteConcern option) {
        WriteConcern applied = option != null ? option : mapper.getWriteConcern(type);
        return applied != null
               ? collection.withWriteConcern(applied)
               : collection;
    }

    /**
     * Sets the Mapper this Datastore uses
     *
     * @param mapper the new Mapper
     */
    public void setMapper(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AggregationPipeline createAggregation(final String collection, final Class<?> clazz) {
        return new AggregationPipelineImpl(this, getDatabase().getCollection(collection), clazz);
    }

    @Override
    public <T> Query<T> createQuery(final String collection, final Class<T> type) {
        return newQuery(type);
    }

    @Override
    public <T> Query<T> createQuery(final Class<T> clazz, final Document q) {
        return newQuery(clazz, q);
    }

    @Override
    public <T, V> DBRef createRef(final Class<T> clazz, final V id) {
        if (id == null) {
            throw new MappingException("Could not get id for " + clazz.getName());
        }
        return new DBRef(getCollection(clazz).getNamespace().getCollectionName(), id);
    }

    @Override
    public <T> DBRef createRef(final T entity) {
        final Object id = mapper.getId(entity);
        if (id == null) {
            throw new MappingException("Could not get id for " + entity.getClass().getName());
        }
        return createRef(entity.getClass(), id);
    }

    @Override
    public <T> UpdateOperations<T> createUpdateOperations(final Class<T> type, final Document ops) {
        final UpdateOpsImpl<T> upOps = (UpdateOpsImpl<T>) createUpdateOperations(type);
        upOps.setOps(ops);
        return upOps;
    }

    @Override
    public <T> Query<T> find(final String collection) {
        return newQuery(mapper.getClassFromCollection(collection));
    }

    @Override
    public <T> void insert(final T entity) {
        insert(entity, new InsertOneOptions()
                           .writeConcern(mapper.getWriteConcern(entity.getClass())));
    }

    @Override
    public <T> void insert(final T entity, final InsertOneOptions options) {
        insert(getCollection(entity.getClass()), entity, options);
    }

    @Override
    public <T> void insert(final List<T> entities, final InsertManyOptions options) {
        if (!entities.isEmpty()) {
            Class<?> type = entities.get(0).getClass();
            MappedClass mappedClass = mapper.getMappedClass(type);
            final MongoCollection collection = getCollection(type);
            MappedField versionField = mappedClass.getVersionField();
            if (versionField != null) {
                for (final T entity : entities) {
                    setInitialVersion(versionField, entity);
                }
            }

            enforceWriteConcern(collection, type, options.getWriteConcern())
                .insertMany(entities, options.getOptions());
        }
    }

    @Override
    public <T> Query<T> queryByExample(final String collection, final T ex) {
        return queryByExample(ex);
    }

    protected <T> void insert(final MongoCollection collection, final T entity, final InsertOneOptions options) {
        setInitialVersion(mapper.getMappedClass(entity.getClass()).getVersionField(), entity);
        mapper.enforceWriteConcern(collection, entity.getClass())
              .insertOne(entity, options.getOptions());
    }

    private <T> void save(final MongoCollection collection, final T entity, final InsertOneOptions options) {
        if (entity == null) {
            throw new UpdateException("Can not persist a null entity");
        }

        final MappedClass mc = mapper.getMappedClass(entity.getClass());

        if (!tryVersionedUpdate(collection, entity, options, mc)) {
            saveDocument(entity, collection, options);
        }
    }

    private <T> boolean tryVersionedUpdate(final MongoCollection collection, final T entity,
                                           final InsertOneOptions options, final MappedClass mc) {
        if (mc.getVersionField() == null) {
            return false;
        }

        MappedField idField = mc.getIdField();
        final Object idValue = idField.getFieldValue(entity);
        final MappedField versionField = mc.getVersionField();

        Long oldVersion = (Long) versionField.getFieldValue(entity);
        long newVersion = oldVersion == null ? 1L : oldVersion + 1;

        if (newVersion == 1) {
            try {
                updateVersion(entity, versionField, newVersion);
                options.apply(collection)
                       .insertOne(entity, options.getOptions());
            } catch (MongoWriteException e) {
                updateVersion(entity, versionField, oldVersion);
                throw new ConcurrentModificationException(format("Entity of class %s (id='%s') was concurrently saved.",
                    entity.getClass().getName(), idValue));
            }
        } else if (idValue != null) {
            final UpdateResult res = find(collection.getNamespace().getCollectionName())
                                         .filter("_id", idValue)
                                         .filter(versionField.getMappedFieldName(), oldVersion)
                                         .update()
                                         .set(entity)
                                         .execute(new UpdateOptions()
                                                      .bypassDocumentValidation(options.getBypassDocumentValidation())
                                                      .writeConcern(options.getWriteConcern()));

            if (res.getModifiedCount() != 1) {
                throw new ConcurrentModificationException(format("Entity of class %s (id='%s',version='%d') was concurrently updated.",
                    entity.getClass().getName(), idValue, oldVersion));
            }
            updateVersion(entity, versionField, newVersion);
        }

        return true;
    }

    protected <T> void saveDocument(final T entity,
                                  final MongoCollection<T> collection,
                                  final InsertOneOptions options) {
        Object id = mapper.getMappedClass(entity.getClass()).getIdField().getFieldValue(entity);
        if (id == null) {
            if (options.clientSession() == null) {
                options.apply(collection)
                       .insertOne(entity, options.getOptions());
            } else {
                options.apply(collection)
                       .insertOne(options.clientSession(), entity, options.getOptions());
            }
        } else {
            ReplaceOptions updateOptions = new ReplaceOptions()
                                               .bypassDocumentValidation(options.getBypassDocumentValidation())
                                               .upsert(true);
            MongoCollection<T> updated = collection;
            if (options.writeConcern() != null) {
                updated = collection.withWriteConcern(options.writeConcern());
            }
            if (options.clientSession() == null) {
                updated.replaceOne(new Document("_id", id), entity, updateOptions);
            } else {
                updated.replaceOne(options.clientSession(), new Document("_id", id), entity, updateOptions);
            }
        }
    }

    private <T> void updateVersion(final T entity, final MappedField field, final Long newVersion) {
        field.setFieldValue(entity, newVersion);
    }

    private <T> void setInitialVersion(final MappedField versionField, final T entity) {
        if (versionField != null) {
            Object value = versionField.getFieldValue(entity);
            if (value != null && !value.equals(0)) {
                throw new ValidationException(Sofia.versionManuallySet());
            } else {
                versionField.setFieldValue(entity, 1L);
            }
        }
    }

    protected <T> Query<T> newQuery(final Class<T> type, final Document query) {
        return getQueryFactory().createQuery(this, type, query);
    }

    protected <T> Query<T> newQuery(final Class<T> type) {
        return getQueryFactory().createQuery(this, type);
    }

    void process(final MappedClass mc, final Validation validation) {
        if (validation != null) {
            String collectionName = mc.getCollectionName();
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
    }
}
