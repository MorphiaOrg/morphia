package dev.morphia;

import com.mongodb.DBRef;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.aggregation.AggregationPipelineImpl;
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.NotSaved;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.Validation;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.mapping.lazy.proxy.ProxyHelper;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.QueryFactory;
import dev.morphia.query.QueryImpl;
import dev.morphia.query.UpdateException;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateOpsImpl;
import org.bson.BsonValue;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.mongodb.DBCollection.ID_FIELD_NAME;
import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.bson.Document.parse;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @morphia.internal
 */
class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final IndexHelper indexHelper;
    private Mapper mapper;

    private volatile QueryFactory queryFactory = new DefaultQueryFactory();

    /**
     * Create a new DatastoreImpl
     *
     * @param mongoClient the connection to the MongoDB instance
     * @param dbName      the name of the database for this data store.
     */
    DatastoreImpl(final MongoClient mongoClient, final MapperOptions options, final String dbName) {
        this.mongoClient = mongoClient;

        this.database = mongoClient.getDatabase(dbName)
                                   .withCodecRegistry(fromRegistries(mongoClient.getMongoClientOptions().getCodecRegistry(),
                                       getDefaultCodecRegistry()));
        this.mapper = new Mapper(this, mongoClient.getMongoClientOptions().getCodecRegistry(), options);
        this.indexHelper = new IndexHelper(mapper, database);
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
    public AggregationPipeline createAggregation(final String collection, final Class<?> clazz) {
        return new AggregationPipelineImpl(this, getDatabase().getCollection(collection), clazz);
    }

    @Override
    public <T> Query<T> createQuery(final String collection, final Class<T> type) {
        return newQuery(type, getDatabase().getCollection(collection));
    }

    @Override
    public <T> Query<T> createQuery(final Class<T> clazz, final Document q) {
        return newQuery(clazz, getCollection(clazz), q);
    }

    @Override
    public <T> DeleteResult delete(final T entity) {
        return delete(entity, new DeleteOptions().writeConcern(getWriteConcern(entity)));
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
        final T wrapped = ProxyHelper.unwrap(entity);
        if (wrapped instanceof Class<?>) {
            throw new MappingException("Did you mean to delete all documents? -- delete(ds.createQuery(???.class))");
        }
        return find(wrapped.getClass()).filter("_id", mapper.getId(wrapped)).remove(options);
    }

    @Override
    public void ensureCaps() {
        ArrayList<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        for (final MappedClass mc : mapper.getMappedClasses()) {
            if (mc.getEntityAnnotation() != null && mc.getEntityAnnotation().cap().value() > 0) {
                final CappedAt cap = mc.getEntityAnnotation().cap();
                final String collName = mapper.getCollectionName(mc.getClazz());
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

    void process(final MappedClass mc, final Validation validation) {
        if (validation != null) {
            String collectionName = mc.getCollectionName();
            Document result = getDatabase()
                                  .runCommand(new Document("collMod", collectionName)
                                                  .append("validator", parse(validation.value()))
                                                  .append("validationLevel", validation.level().getValue())
                                                  .append("validationAction", validation.action().getValue())
                                             );

            throw new UnsupportedOperationException("update from the command result");
/*
            if (!result.ok()) {
                if (result.getInt("code") == 26) {
                    ValidationOptions options = new ValidationOptions()
                                                    .validator(parse(validation.value()))
                                                    .validationLevel(validation.level())
                                                    .validationAction(validation.action());
                    getDatabase().createCollection(collectionName, new CreateCollectionOptions().validationOptions(options));
                } else {
                    result.throwOnError();
                }
            }
*/
        }
    }

    @Override
    public <T> Query<T> find(final Class<T> clazz) {
        return newQuery(clazz, getCollection(clazz));
    }

    @Override
    public <T> Query<T> find(final String collection) {
        return newQuery(mapper.getClassFromCollection(collection), getDatabase().getCollection(collection));
    }

    @Override
    public <T, V> Query<T> get(final Class<T> clazz, final Iterable<V> ids) {
        return find(clazz).disableValidation().filter("_id" + " in", ids).enableValidation();
    }

    @Override
    public <T> T getByKey(final Class<T> clazz, final Key<T> key) {
        final String collectionName = mapper.getCollectionName(clazz);
        final String keyCollection = mapper.updateCollection(key);
        if (!collectionName.equals(keyCollection)) {
            throw new RuntimeException("collection names don't match for key and class: " + collectionName + " != " + keyCollection);
        }

        Object id = key.getId();
        if (id instanceof Document) {
            ((Document) id).remove(mapper.getOptions().getDiscriminatorField());
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

    private MongoCollection<?> getCollection(final Object obj) {
        if (obj == null) {
            return null;
        }
        return getCollection(obj instanceof Class ? (Class) obj : obj.getClass());
    }

    @Override
    public MongoCollection<Document> getCollection(final Class clazz) {
        final String collName = mapper.getCollectionName(clazz);
        return getDatabase().getCollection(collName);
    }

    private <T> MongoCollection<T> getMongoCollection(final Class<T> clazz) {
        return getMongoCollection(mapper.getCollectionName(clazz), clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> MongoCollection<T> getMongoCollection(final String name, final Class<T> clazz) {
        final MongoCollection<T> collection = database.getCollection(name, clazz);
        return enforceWriteConcern(collection, clazz);
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
    public MongoClient getMongo() {
        return mongoClient;
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
    public <T> Key<T> merge(final T entity) {
        return merge(entity, getWriteConcern(entity));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Key<T> merge(final T entity, final WriteConcern wc) {
        T unwrapped = entity;
        final LinkedHashMap<Object, Document> involvedObjects = new LinkedHashMap<>();
        final Document dbObj = mapper.toDocument(unwrapped, involvedObjects);
        final Key<T> key = getKey(unwrapped);
        unwrapped = ProxyHelper.unwrap(unwrapped);
        final Object id = mapper.getId(unwrapped);
        if (id == null) {
            throw new MappingException("Could not get id for " + unwrapped.getClass().getName());
        }

        // remove (immutable) _id field for update.
        final Object idValue = dbObj.get("_id");
        dbObj.remove("_id");

        UpdateResult wr;

        final MappedClass mc = mapper.getMappedClass(unwrapped);
        final MongoCollection collection = getCollection(unwrapped);

        // try to do an update if there is a @Version field
        final Document set = new Document("$set", dbObj);
        wr = tryVersionedUpdate(collection, unwrapped, set, idValue, new InsertOneOptions().writeConcern(wc), mc);

        if (wr == null) {
            final Query<T> query = (Query<T>) find(unwrapped.getClass()).filter("_id", id);
            wr = query.update(set).execute(new UpdateOptions().writeConcern(wc));
        }

        if (wr.getModifiedCount() == 0) {
            throw new UpdateException("Nothing updated");
        }

        dbObj.put("_id", idValue);
        postSaveOperations(List.of(entity), involvedObjects, false, collection.getNamespace().getCollectionName());
        return key;
    }

    @Override
    public <T> Query<T> queryByExample(final T ex) {
        return queryByExample(getCollection(ex), ex);
    }

    @Override
    public <T> Iterable<Key<T>> save(final Iterable<T> entities) {
        Iterator<T> iterator = entities.iterator();
        return !iterator.hasNext()
               ? Collections.emptyList()
               : save(entities, getWriteConcern(iterator.next()));
    }

    private <T> Iterable<Key<T>> save(final Iterable<T> entities, final WriteConcern wc) {
        return save(entities, new InsertOptions().writeConcern(wc));
    }

    @Override
    public <T> Iterable<Key<T>> save(final Iterable<T> entities, final InsertOptions options) {
        final List<Key<T>> savedKeys = new ArrayList<>();
        for (final T ent : entities) {
            savedKeys.add(save(ent, options));
        }
        return savedKeys;

    }

    @Override
    public <T> Key<T> save(final T entity) {
        return save(entity, new InsertOptions());
    }

    @Override
    public <T> Key<T> save(final T entity, final InsertOptions options) {
        if (entity == null) {
            throw new UpdateException("Can not persist a null entity");
        }

        final T unwrapped = ProxyHelper.unwrap(entity);
        return save(getCollection(unwrapped), unwrapped, options.toInsertOneOptions());
    }

    @Override
    public <T> UpdateResult update(final Query<T> query, final UpdateOperations<T> operations) {
        return query.update(operations).execute(new UpdateOptions()
                                                    .upsert(false)
                                                    .multi(true)
                                                    .writeConcern(getWriteConcern(((QueryImpl) query).getEntityClass())));
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
        final T wrapped = ProxyHelper.unwrap(entity);
        final Object id = mapper.getId(wrapped);
        if (id == null) {
            throw new MappingException("Could not get id for " + wrapped.getClass().getName());
        }
        return createRef(wrapped.getClass(), id);
    }

    @Override
    public <T> UpdateOperations<T> createUpdateOperations(final Class<T> type, final Document ops) {
        final UpdateOpsImpl<T> upOps = (UpdateOpsImpl<T>) createUpdateOperations(type);
        upOps.setOps(ops);
        return upOps;
    }

    @Override
    public void ensureIndexes() {
        for (final MappedClass mc : mapper.getMappedClasses()) {
            indexHelper.createIndex(getMongoCollection(mc.getClazz()), mc);
        }
    }

    @Override
    public <T> void ensureIndexes(final Class<T> clazz) {
        indexHelper.createIndex(getMongoCollection(clazz), mapper.getMappedClass(clazz));
    }

    @Override
    public <T> Key<T> insert(final T entity) {
        return insert(entity, getWriteConcern(entity));
    }

    private <T> Key<T> insert(final T entity, final WriteConcern wc) {
        return insert(entity, new InsertOptions().writeConcern(wc));
    }

    @Override
    public <T> Key<T> insert(final T entity, final InsertOneOptions options) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        return insert(getCollection(unwrapped), unwrapped, options);
    }

    @Override
    public <T> Iterable<Key<T>> insert(final List<T> entities, final InsertManyOptions options) {
        return entities.isEmpty()
               ? Collections.emptyList()
               : insert(getCollection(entities.get(0)), entities, options);
    }

    @Override
    public <T> Query<T> queryByExample(final String collection, final T ex) {
        return queryByExample(getDatabase().getCollection(collection), ex);
    }

    /**
     * @return the Mapper used by this Datastore
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Sets the Mapper this Datastore uses
     *
     * @param mapper the new Mapper
     */
    public void setMapper(final Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Inserts an entity in to the database
     *
     * @param collection the collection to query against
     * @param entity     the entity to insert
     * @param wc         the WriteConcern to use when deleting
     * @param <T>        the type of the entities
     * @return the key of entity
     */
    public <T> Key<T> insert(final String collection, final T entity, final WriteConcern wc) {
        return insert(getCollection(collection), ProxyHelper.unwrap(entity), new InsertOneOptions().writeConcern(wc));
    }

    private MongoCollection getCollection(final String kind) {
        if (kind == null) {
            return null;
        }
        return getDatabase().getCollection(kind);
    }

    @Deprecated
    protected Object getId(final Object entity) {
        return mapper.getId(entity);
    }

    protected <T> Key<T> insert(final MongoCollection collection, final T entity, final InsertOneOptions options) {
        final LinkedHashMap<Object, Document> involvedObjects = new LinkedHashMap<>();
        enforceWriteConcern(collection, entity.getClass())
            .insertOne(singletonList(entityToDocument(entity, involvedObjects)), options.getOptions());

        return postSaveOperations(singletonList(entity), involvedObjects, collection.getNamespace().getCollectionName()).get(0);
    }

    protected <T> Key<T> save(final MongoCollection collection, final T entity, final InsertOneOptions options) {
        final MappedClass mc = validateSave(entity);

        // involvedObjects is used not only as a cache but also as a list of what needs to be called for life-cycle methods at the end.
        final LinkedHashMap<Object, Document> involvedObjects = new LinkedHashMap<>();
        final Document document = entityToDocument(entity, involvedObjects);

        // try to do an update if there is a @Version field
        final Object idValue = document.get("_id");
        UpdateResult updateResult = tryVersionedUpdate(collection, entity, document, idValue, options, mc);

        if (updateResult == null) {
            saveDocument(collection, document, options, entity.getClass());
        }

        return postSaveOperations(singletonList(entity), involvedObjects, collection.getNamespace().getCollectionName()).get(0);
    }

/*
    @SuppressWarnings("unchecked")
    private <T> Key<T> save(final MongoCollection collection, final T entity, final InsertOneOptions options) {
        final MappedClass mc = validateSave(entity);

        // involvedObjects is used not only as a cache but also as a list of what needs to be called for life-cycle methods at the end.
        final LinkedHashMap<Object, Document> involvedObjects = new LinkedHashMap<Object, Document>();
        final Document document = new Document(entityToDBObj(entity, involvedObjects).toMap());

        // try to do an update if there is a @Version field
        final Object idValue = document.get(Mapper.ID_KEY);
        UpdateResult wr = tryVersionedUpdate(collection, entity, document, idValue, options, mc);

        if (wr == null) {
            if (document.get(ID_FIELD_NAME) == null) {
                 collection.insertOne(singletonList(document), options);
            } else {
                collection.updateOne(new Document(ID_FIELD_NAME, document.get(ID_FIELD_NAME)), document,
                    new com.mongodb.client.model.UpdateOptions()
                        .bypassDocumentValidation(options.getBypassDocumentValidation())
                        .upsert(true));
            }
        }

        return postSaveOperations(singletonList(entity), involvedObjects, collection.getNamespace().getCollectionName()).get(0);
    }
*/

    private <T> MappedClass validateSave(final T entity) {
        if (entity == null) {
            throw new UpdateException("Can not persist a null entity");
        }

        final MappedClass mc = mapper.getMappedClass(entity);
        if (mc.getAnnotation(NotSaved.class) != null) {
            throw new MappingException(format("Entity type: %s is marked as NotSaved which means you should not try to save it!",
                mc.getClazz().getName()));
        }
        return mc;
    }

    private UpdateResult saveDocument(final MongoCollection collection, final Document document, final InsertOneOptions options,
                                      final Class<?> clazz) {
        if (document.get(ID_FIELD_NAME) == null) {
            collection.insertOne(document, options.getOptions());
            return new MockedUpdateResult(options.getWriteConcern());
        } else {
            UpdateOptions updateOptions = new UpdateOptions()
                                              .bypassDocumentValidation(options.getBypassDocumentValidation())
                                              .writeConcern(options.getWriteConcern())
                                              .upsert(true);
            Document filter = new Document(ID_FIELD_NAME, document.get(ID_FIELD_NAME));
            return enforceWriteConcern(collection, clazz, updateOptions.getWriteConcern())
                       .updateOne(filter, document, updateOptions);
        }
    }

    private <T> UpdateResult tryVersionedUpdate(final MongoCollection collection, final T entity, final Document document,
                                                final Object idValue, final InsertOneOptions options, final MappedClass mc) {
        UpdateResult updateResult = null;
        if (mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            return null;
        }

        final MappedField mfVersion = mc.getMappedVersionField();
        final String versionKeyName = mfVersion.getMappedFieldName();

        Long oldVersion = (Long) mfVersion.getFieldValue(entity);
        long newVersion = nextValue(oldVersion);

        final Document set = (Document) document.get("$set");
        if (set == null) {
            document.put(versionKeyName, newVersion);
        } else {
            set.put(versionKeyName, newVersion);
        }

        if (idValue != null && newVersion == 1) {
            try {
                collection.insertOne(document, options.getOptions());
                updateResult = new MockedUpdateResult(options.getWriteConcern());
            } catch (DuplicateKeyException e) {
                throw new ConcurrentModificationException(format("Entity of class %s (id='%s') was concurrently saved.",
                    entity.getClass().getName(), idValue));
            }
        } else if (idValue != null) {
            final QueryImpl<?> query = (QueryImpl<?>) find(collection.getNamespace().getCollectionName())
                                                          .disableValidation()
                                                          .filter("_id", idValue)
                                                          .enableValidation()
                                                          .filter(versionKeyName, oldVersion);
            final UpdateResult res = query.update(document).execute(new UpdateOptions()
                                                                        .bypassDocumentValidation(options.getBypassDocumentValidation())
                                                                        .writeConcern(options.getWriteConcern()));

            if (res.getModifiedCount() != 1) {
                throw new ConcurrentModificationException(format("Entity of class %s (id='%s',version='%d') was concurrently updated.",
                    entity.getClass().getName(), idValue, oldVersion));
            }
        } else {
            updateResult = saveDocument(collection, document, options, entity.getClass());
        }

        return updateResult;
    }

    private EntityCache createCache() {
        return mapper.createEntityCache();
    }

    private Document entityToDocument(final Object entity, final Map<Object, Document> involvedObjects) {
        return mapper.toDocument(ProxyHelper.unwrap(entity), involvedObjects);
    }

    private <T> Iterable<Key<T>> insert(final MongoCollection collection, final List<T> entities, final InsertManyOptions options) {
        if (!entities.iterator().hasNext()) {
            return emptyList();
        }

        final Map<Object, Document> involvedObjects = new LinkedHashMap<>();
        final List<Document> list = new ArrayList<>();
        InsertManyOptions insertOptions = options;
        for (final T entity : entities) {
            list.add(toDocument(entity, involvedObjects));
        }
        enforceWriteConcern(collection, entities.get(0).getClass(), options.getWriteConcern())
            .insertMany(list, insertOptions.getOptions());

        return postSaveOperations(entities, involvedObjects, collection.getNamespace().getCollectionName());
    }

    /**
     * Creates and returns a {@link Query} using the underlying {@link QueryFactory}.
     */
    private <T> Query<T> newQuery(final Class<T> type, final MongoCollection collection, final Document query) {
        return getQueryFactory().createQuery(this, collection, type, query);
    }

    /**
     * Creates and returns a {@link Query} using the underlying {@link QueryFactory}.
     *
     * @see QueryFactory#createQuery(Datastore, MongoCollection, Class)
     */
    private <T> Query<T> newQuery(final Class<T> type, final MongoCollection collection) {
        return getQueryFactory().createQuery(this, collection, type);
    }

    private long nextValue(final Long oldVersion) {
        return oldVersion == null ? 1 : oldVersion + 1;
    }

    private <T> List<Key<T>> postSaveOperations(final Iterable<T> entities,
                                                final Map<Object, Document> involvedObjects,
                                                final String collectionName) {
        return postSaveOperations(entities, involvedObjects, true, collectionName);
    }

    @SuppressWarnings("unchecked")
    private <T> List<Key<T>> postSaveOperations(final Iterable<T> entities, final Map<Object, Document> involvedObjects,
                                                final boolean fetchKeys, final String collectionName) {
        List<Key<T>> keys = new ArrayList<>();
        for (final T entity : entities) {
            final Document dbObj = involvedObjects.remove(entity);

            if (fetchKeys) {
                if (dbObj.get("_id") == null) {
                    throw new MappingException(format("Missing _id after save on %s", entity.getClass().getName()));
                }
                mapper.updateKeyAndVersionInfo(this, dbObj, createCache(), entity);
                keys.add(new Key<>((Class<? extends T>) entity.getClass(), collectionName, mapper.getId(entity)));
            }
            mapper.getMappedClass(entity).callLifecycleMethods(PostPersist.class, entity, dbObj, mapper);
        }

        for (Entry<Object, Document> entry : involvedObjects.entrySet()) {
            final Object key = entry.getKey();
            mapper.getMappedClass(key).callLifecycleMethods(PostPersist.class, key, entry.getValue(), mapper);

        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private <T> Query<T> queryByExample(final MongoCollection coll, final T example) {
        // TODO: think about remove className from baseQuery param below.
        final Class<T> type = (Class<T>) example.getClass();
        final Document query = entityToDocument(example, new HashMap<>());
        return newQuery(type, coll, query);
    }

    private <T> Document toDocument(final T ent, final Map<Object, Document> involvedObjects) {
        final MappedClass mc = mapper.getMappedClass(ent);
        if (mc.getAnnotation(NotSaved.class) != null) {
            throw new MappingException(format("Entity type: %s is marked as NotSaved which means you should not try to save it!",
                mc.getClazz().getName()));
        }
        Document document = entityToDocument(ent, involvedObjects);
        List<MappedField> versionFields = mc.getFieldsAnnotatedWith(Version.class);
        for (MappedField mappedField : versionFields) {
            String name = mappedField.getMappedFieldName();
            if (document.get(name) == null) {
                document.put(name, 1);
                mappedField.setFieldValue(ent, 1L);
            }
        }
        return document;
    }

/*
    @SuppressWarnings("unchecked")
    private <T> UpdateResults update(final QueryImpl<T> query, final Document update, final UpdateOptions options) {

        MongoCollection dbColl = query.getCollection();
        // TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(query.getEntityClass());
        }

        if (query.getSort() != null && query.getSort().keySet() != null && !query.getSort().keySet().isEmpty()) {
            throw new QueryException("sorting is not allowed for updates.");
        }

        Document queryObject = query.getQueryDocument();

        final MappedClass mc = getMapper().getMappedClass(query.getEntityClass());
        final List<MappedField> fields = mc.getFieldsAnnotatedWith(Version.class);
        if (!fields.isEmpty()) {
            final MappedField versionMF = fields.get(0);
            Document localUpdate = update;
            if (localUpdate.get("$set") != null) {
                localUpdate = (Document) localUpdate.get("$set");
            }
            if (localUpdate.get(versionMF.getNameToStore()) == null) {
                if (!localUpdate.containsKey("$inc")) {
                    localUpdate.put("$inc", new Document(versionMF.getNameToStore(), 1));
                } else {
                    ((Map<String, Object>) (localUpdate.get("$inc"))).put(versionMF.getNameToStore(), 1);
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Executing update(%s) for query: %s, ops: %s, multi: %s, upsert: %s",
                dbColl.getNamespace().getCollectionName(), queryObject, update, options.isMulti(), options.isUpsert()));
        }

        return null; //new UpdateResults(dbColl.updateMany(queryObject, update,
//            enforceWriteConcern(options, query.getEntityClass())
//                .getOptions()));
    }
*/


    public MongoCollection enforceWriteConcern(final MongoCollection collection, final Class klass, WriteConcern option) {
        WriteConcern applied = option != null ? option : getWriteConcern(klass);
        return applied != null
               ? collection.withWriteConcern(applied)
               : collection;
    }

    private MongoCollection enforceWriteConcern(final MongoCollection collection, final Class klass) {
        WriteConcern applied = getWriteConcern(klass);
        return applied != null
               ? collection.withWriteConcern(applied)
               : collection;
    }


    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazzOrEntity the class or entity to use when looking up the WriteConcern
     */
    private WriteConcern getWriteConcern(final Object clazzOrEntity) {
        WriteConcern wc = getMongo().getWriteConcern();
        if (clazzOrEntity != null) {
            final Entity entityAnn = getMapper().getMappedClass(clazzOrEntity).getEntityAnnotation();
            if (entityAnn != null && !entityAnn.concern().isEmpty()) {
                wc = WriteConcern.valueOf(entityAnn.concern());
            }
        }

        return wc;
    }

    private static class MockedUpdateResult extends UpdateResult {
        private WriteConcern writeConcern;

        public MockedUpdateResult(final WriteConcern writeConcern) {
            this.writeConcern = writeConcern;
        }

        @Override
        public boolean wasAcknowledged() {
            return writeConcern.equals(WriteConcern.ACKNOWLEDGED);
        }

        @Override
        public long getMatchedCount() {
            return 1;
        }

        @Override
        public boolean isModifiedCountAvailable() {
            return true;
        }

        @Override
        public long getModifiedCount() {
            return 1;
        }

        @Override
        public BsonValue getUpsertedId() {
            return null;
        }
    }
}
