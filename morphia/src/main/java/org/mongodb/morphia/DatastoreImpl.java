package org.mongodb.morphia;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.DefaultDBDecoder;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.AggregationPipelineImpl;
import org.mongodb.morphia.annotations.CappedAt;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.annotations.Text;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.mapping.lazy.proxy.ProxyHelper;
import org.mongodb.morphia.query.DefaultQueryFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryException;
import org.mongodb.morphia.query.QueryFactory;
import org.mongodb.morphia.query.UpdateException;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateOpsImpl;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.utils.Assert;
import org.mongodb.morphia.utils.IndexType;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mongodb.morphia.AnnotationUtils.convert;
import static org.mongodb.morphia.AnnotationUtils.field;
import static org.mongodb.morphia.AnnotationUtils.synthesizeIndex;
import static org.mongodb.morphia.utils.IndexType.fromValue;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @author Scott Hernandez
 */
@SuppressWarnings("deprecation")
public class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = MorphiaLoggerFactory.get(DatastoreImpl.class);
    private static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();

    private final Morphia morphia;
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private DB db;
    private Mapper mapper;
    private WriteConcern defConcern = WriteConcern.ACKNOWLEDGED;
    private DBDecoderFactory decoderFactory;

    private volatile QueryFactory queryFactory = new DefaultQueryFactory();

    DatastoreImpl(final Morphia morphia, final MongoClient mongoClient, final String dbName) {
        this(morphia, morphia.getMapper(), mongoClient, dbName);
    }

    DatastoreImpl(final Morphia morphia, final Mapper mapper, final MongoClient mongoClient, final String dbName) {
        this(morphia, mapper, mongoClient, mongoClient.getDatabase(dbName));
    }

    private DatastoreImpl(final Morphia morphia, final Mapper mapper, final MongoClient mongoClient, final MongoDatabase database) {
        this.morphia = morphia;
        this.mapper = mapper;
        this.mongoClient = mongoClient;
        this.database = database;
    }

    /**
     * Creates a copy of this Datastore and all its configuration but with a new database
     *
     * @param database the new database to use for operations
     * @return the new Datastore instance
     */
    public DatastoreImpl copy(final String database) {
        return new DatastoreImpl(morphia, mapper, mongoClient, database);
    }

    /**
     * @param source the initial type/collection to aggregate against
     * @return a new query bound to the kind (a specific {@link DBCollection})
     */
    @Override
    public AggregationPipeline createAggregation(final Class source) {
        return new AggregationPipelineImpl(this, getCollection(source), source);
    }

    @Override
    public AggregationPipeline createAggregation(final String collection, final Class<?> clazz) {
        return new AggregationPipelineImpl(this, getDB().getCollection(collection), clazz);
    }

    @Override
    public <T> Query<T> createQuery(final Class<T> collection) {
        return newQuery(collection, getCollection(collection));
    }

    @Override
    public <T> UpdateOperations<T> createUpdateOperations(final Class<T> clazz) {
        return new UpdateOpsImpl<T>(clazz, getMapper());
    }

    @Override
    public <T, V> WriteResult delete(final Class<T> clazz, final V id) {
        return delete(clazz, id, getWriteConcern(clazz));
    }

    @Override
    public <T, V> WriteResult delete(final Class<T> clazz, final Iterable<V> ids) {
        final Query<T> q = find(clazz).filter(Mapper.ID_KEY + " in", ids);
        return delete(q);
    }

    @Override
    public <T> WriteResult delete(final Query<T> query) {
        return delete(query, getWriteConcern(query.getEntityClass()));
    }

    @Override
    public <T> WriteResult delete(final Query<T> query, final WriteConcern wc) {

        DBCollection dbColl = query.getCollection();
        // TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(query.getEntityClass());
        }

        final WriteResult wr;

        if (query.getSortObject() != null || query.getOffset() != 0 || query.getLimit() > 0) {
            throw new QueryException("Delete does not allow sort/offset/limit query options.");
        }

        final DBObject queryObject = query.getQueryObject();
        if (queryObject != null) {
            if (wc == null) {
                wr = dbColl.remove(queryObject);
            } else {
                wr = dbColl.remove(queryObject, wc);
            }
        } else if (wc == null) {
            wr = dbColl.remove(new BasicDBObject());
        } else {
            wr = dbColl.remove(new BasicDBObject(), wc);
        }

        return wr;
    }

    @Override
    public <T> WriteResult delete(final T entity) {
        return delete(entity, getWriteConcern(entity));
    }

    @Override
    public <T> WriteResult delete(final T entity, final WriteConcern wc) {
        final T wrapped = ProxyHelper.unwrap(entity);
        if (wrapped instanceof Class<?>) {
            throw new MappingException("Did you mean to delete all documents? -- delete(ds.createQuery(???.class))");
        }
        try {
            final Object id = mapper.getId(wrapped);
            return delete(wrapped.getClass(), id, wc);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void ensureCaps() {
        for (final MappedClass mc : mapper.getMappedClasses()) {
            if (mc.getEntityAnnotation() != null && mc.getEntityAnnotation().cap().value() > 0) {
                final CappedAt cap = mc.getEntityAnnotation().cap();
                final String collName = mapper.getCollectionName(mc.getClazz());
                final BasicDBObjectBuilder dbCapOpts = BasicDBObjectBuilder.start("capped", true);
                if (cap.value() > 0) {
                    dbCapOpts.add("size", cap.value());
                }
                if (cap.count() > 0) {
                    dbCapOpts.add("max", cap.count());
                }
                final DB database = getDB();
                if (database.getCollectionNames().contains(collName)) {
                    final DBObject dbResult = database.command(BasicDBObjectBuilder.start("collstats", collName).get());
                    if (dbResult.containsField("capped")) {
                        // TODO: check the cap options.
                        LOG.debug("DBCollection already exists and is capped already; doing nothing. " + dbResult);
                    } else {
                        LOG.warning("DBCollection already exists with same name(" + collName
                                        + ") and is not capped; not creating capped version!");
                    }
                } else {
                    getDB().createCollection(collName, dbCapOpts.get());
                    LOG.debug("Created capped DBCollection (" + collName + ") with opts " + dbCapOpts);
                }
            }
        }
    }

    @Override
    public Key<?> exists(final Object entityOrKey) {
        final Query<?> query = buildExistsQuery(entityOrKey);
        return query.getKey();
    }

    @Override
    public <T> Query<T> find(final Class<T> clazz) {
        return createQuery(clazz);
    }

    @Override
    public <T, V> Query<T> find(final Class<T> clazz, final String property, final V value) {
        final Query<T> query = createQuery(clazz);
        return query.filter(property, value);
    }

    @Override
    public <T, V> Query<T> find(final Class<T> clazz, final String property, final V value, final int offset, final int size) {
        final Query<T> query = createQuery(clazz);
        query.offset(offset);
        query.limit(size);
        return query.filter(property, value);
    }

    @Override
    public <T> T findAndDelete(final Query<T> query) {
        DBCollection dbColl = query.getCollection();
        // TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(query.getEntityClass());
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing findAndModify(" + dbColl.getName() + ") with delete ...");
        }

        final DBObject result = dbColl.findAndModify(query.getQueryObject(), query.getFieldsObject(), query.getSortObject(), true,
                                                     null, false, false);

        if (result != null) {
            return mapper.fromDBObject(this, query.getEntityClass(), result, createCache());
        }

        return null;
    }

    @Override
    public <T> T findAndModify(final Query<T> query, final UpdateOperations<T> operations) {
        return findAndModify(query, operations, false);
    }

    @Override
    public <T> T findAndModify(final Query<T> query, final UpdateOperations<T> operations, final boolean oldVersion) {
        return findAndModify(query, operations, oldVersion, false);
    }

    @Override
    public <T> T findAndModify(final Query<T> query, final UpdateOperations<T> operations, final boolean oldVersion,
                               final boolean createIfMissing) {

        DBCollection dbColl = query.getCollection();
        // TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(query.getEntityClass());
        }

        if (LOG.isTraceEnabled()) {
            LOG.info("Executing findAndModify(" + dbColl.getName() + ") with update ");
        }
        DBObject res = null;
        try {
            updateForVersioning(query, operations);
            res = dbColl.findAndModify(query.getQueryObject(), query.getFieldsObject(), query.getSortObject(), false,
                                       ((UpdateOpsImpl<T>) operations).getOps(), !oldVersion, createIfMissing);
        } catch (MongoException e) {
            if (e.getMessage() == null || !e.getMessage().contains("matching")) {
                throw e;
            }
        }

        return res == null ? null : mapper.fromDBObject(this, query.getEntityClass(), res, createCache());
    }

    private <T> void updateForVersioning(final Query<T> query, final UpdateOperations<T> operations) {
        final MappedClass mc = mapper.getMappedClass(query.getEntityClass());

        if (!mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            MappedField versionField = mc.getMappedVersionField();
            operations.inc(versionField.getNameToStore());
        }

    }

    @Override
    public <T, V> Query<T> get(final Class<T> clazz, final Iterable<V> ids) {
        return find(clazz).disableValidation().filter(Mapper.ID_KEY + " in", ids).enableValidation();
    }

    @Override
    public <T, V> T get(final Class<T> clazz, final V id) {
        return find(getCollection(clazz).getName(), clazz, Mapper.ID_KEY, id, 0, 1, true).get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final T entity) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        final Object id = mapper.getId(unwrapped);
        if (id == null) {
            throw new MappingException("Could not get id for " + unwrapped.getClass().getName());
        }
        return (T) get(unwrapped.getClass(), id);
    }

    @Override
    public <T> T getByKey(final Class<T> clazz, final Key<T> key) {
        final String collectionName = mapper.getCollectionName(clazz);
        final String keyCollection = mapper.updateCollection(key);
        if (!collectionName.equals(keyCollection)) {
            throw new RuntimeException("collection names don't match for key and class: " + collectionName + " != " + keyCollection);
        }

        Object id = key.getId();
        if (id instanceof DBObject) {
            ((DBObject) id).removeField(Mapper.CLASS_NAME_FIELDNAME);
        }
        return get(clazz, id);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> List<T> getByKeys(final Class<T> clazz, final Iterable<Key<T>> keys) {

        final Map<String, List<Key>> kindMap = new HashMap<String, List<Key>>();
        final List<T> entities = new ArrayList<T>();
        // String clazzKind = (clazz==null) ? null :
        // getMapper().getCollectionName(clazz);
        for (final Key<?> key : keys) {
            mapper.updateCollection(key);

            // if (clazzKind != null && !key.getKind().equals(clazzKind))
            // throw new IllegalArgumentException("Types are not equal (" +
            // clazz + "!=" + key.getKindClass() +
            // ") for key and method parameter clazz");
            //
            if (kindMap.containsKey(key.getCollection())) {
                kindMap.get(key.getCollection()).add(key);
            } else {
                kindMap.put(key.getCollection(), new ArrayList<Key>(singletonList((Key) key)));
            }
        }
        for (final Map.Entry<String, List<Key>> entry : kindMap.entrySet()) {
            final List<Key> kindKeys = entry.getValue();

            final List<Object> objIds = new ArrayList<Object>();
            for (final Key key : kindKeys) {
                objIds.add(key.getId());
            }
            final List kindResults = find(entry.getKey(), null).disableValidation().filter("_id in", objIds).asList();
            entities.addAll(kindResults);
        }

        // TODO: order them based on the incoming Keys.
        return entities;
    }

    @Override
    public <T> List<T> getByKeys(final Iterable<Key<T>> keys) {
        return getByKeys(null, keys);
    }

    private DBCollection getCollection(final Object obj) {
        if (obj == null) {
            return null;
        }
        return getCollection(obj instanceof Class ? (Class) obj : obj.getClass());
    }

    @Override
    public DBCollection getCollection(final Class clazz) {
        final String collName = mapper.getCollectionName(clazz);
        return getDB().getCollection(collName);
    }

    @Override
    public MongoCollection getMongoCollection(final Class clazz) {
        return getMongoCollection(mapper.getCollectionName(clazz));
    }

    private MongoCollection getMongoCollection(final String name) {
        return database.getCollection(name);
    }

    @Override
    public <T> long getCount(final T entity) {
        return getCollection(ProxyHelper.unwrap(entity)).count();
    }

    @Override
    public <T> long getCount(final Class<T> clazz) {
        return getCollection(clazz).count();
    }

    @Override
    public <T> long getCount(final Query<T> query) {
        return query.countAll();
    }

    @Override
    public DB getDB() {
        return db != null ? db : mongoClient.getDB(database.getName());
    }

    @Override
    public WriteConcern getDefaultWriteConcern() {
        return defConcern;
    }

    @Override
    public void setDefaultWriteConcern(final WriteConcern wc) {
        defConcern = wc;
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
    public <T> MapreduceResults<T> mapReduce(final MapreduceType type, final Query query, final String map, final String reduce,
                                             final String finalize, final Map<String, Object> scopeFields, final Class<T> outputType) {

        final DBCollection dbColl = query.getCollection();

        final String outColl = mapper.getCollectionName(outputType);

        final OutputType outType;
        switch (type) {
            case REDUCE:
                outType = OutputType.REDUCE;
                break;
            case MERGE:
                outType = OutputType.MERGE;
                break;
            case INLINE:
                outType = OutputType.INLINE;
                break;
            default:
                outType = OutputType.REPLACE;
                break;
        }

        final MapReduceCommand cmd = new MapReduceCommand(dbColl, map, reduce, outColl, outType, query.getQueryObject());

        if (query.getLimit() > 0) {
            cmd.setLimit(query.getLimit());
        }
        if (query.getSortObject() != null) {
            cmd.setSort(query.getSortObject());
        }

        if (finalize != null && finalize.length() != 0) {
            cmd.setFinalize(finalize);
        }

        if (scopeFields != null && !scopeFields.isEmpty()) {
            cmd.setScope(scopeFields);
        }

        return mapReduce(type, query, outputType, cmd);
    }

    @Override
    public <T> MapreduceResults<T> mapReduce(final MapreduceType type, final Query query, final Class<T> outputType,
                                             final MapReduceCommand baseCommand) {

        Assert.parametersNotNull("map", baseCommand.getMap());
        Assert.parameterNotEmpty("map", baseCommand.getMap());
        Assert.parametersNotNull("reduce", baseCommand.getReduce());
        Assert.parameterNotEmpty("reduce", baseCommand.getReduce());

        if (query.getOffset() != 0 || query.getFieldsObject() != null) {
            throw new QueryException("mapReduce does not allow the offset/retrievedFields query options.");
        }

        final OutputType outType;
        switch (type) {
            case REDUCE:
                outType = OutputType.REDUCE;
                break;
            case MERGE:
                outType = OutputType.MERGE;
                break;
            case INLINE:
                outType = OutputType.INLINE;
                break;
            default:
                outType = OutputType.REPLACE;
                break;
        }

        final DBCollection dbColl = query.getCollection();

        final MapReduceCommand cmd = new MapReduceCommand(dbColl, baseCommand.getMap(), baseCommand.getReduce(),
                                                          baseCommand.getOutputTarget(), outType, query.getQueryObject());
        cmd.setFinalize(baseCommand.getFinalize());
        cmd.setScope(baseCommand.getScope());

        if (query.getLimit() > 0) {
            cmd.setLimit(query.getLimit());
        }
        if (query.getSortObject() != null) {
            cmd.setSort(query.getSortObject());
        }

        if (LOG.isTraceEnabled()) {
            LOG.info("Executing " + cmd.toString());
        }

        final EntityCache cache = createCache();
        MapreduceResults<T> results = new MapreduceResults<T>(dbColl.mapReduce(baseCommand));

        results.setType(type);
        if (MapreduceType.INLINE.equals(type)) {
            results.setInlineRequiredOptions(this, outputType, getMapper(), cache);
        } else {
            results.setQuery(newQuery(outputType, getDB().getCollection(results.getOutputCollectionName())));
        }

        return results;

    }

    @Override
    public <T> Key<T> merge(final T entity) {
        return merge(entity, getWriteConcern(entity));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Key<T> merge(final T entity, final WriteConcern wc) {
        T unwrapped = entity;
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = mapper.toDBObject(unwrapped, involvedObjects);
        final Key<T> key = mapper.getKey(unwrapped);
        unwrapped = ProxyHelper.unwrap(unwrapped);
        final Object id = mapper.getId(unwrapped);
        if (id == null) {
            throw new MappingException("Could not get id for " + unwrapped.getClass().getName());
        }

        // remove (immutable) _id field for update.
        final Object idValue = dbObj.get(Mapper.ID_KEY);
        dbObj.removeField(Mapper.ID_KEY);

        WriteResult wr;

        final MappedClass mc = mapper.getMappedClass(unwrapped);
        final DBCollection dbColl = getCollection(unwrapped);

        // try to do an update if there is a @Version field
        wr = tryVersionedUpdate(dbColl, unwrapped, dbObj, idValue, wc, mc);

        if (wr == null) {
            final Query<T> query = (Query<T>) createQuery(unwrapped.getClass()).filter(Mapper.ID_KEY, id);
            wr = update(query, new BasicDBObject("$set", dbObj), false, false, wc).getWriteResult();
        }

        final UpdateResults res = new UpdateResults(wr);

        if (res.getUpdatedCount() == 0) {
            throw new UpdateException("Nothing updated");
        }

        dbObj.put(Mapper.ID_KEY, idValue);
        postSaveOperations(Collections.<Object>singletonList(entity), involvedObjects, dbColl, false);
        return key;
    }

    @Override
    public <T> Query<T> queryByExample(final T ex) {
        return queryByExample(getCollection(ex), ex);
    }

    @Override
    public <T> Iterable<Key<T>> save(final Iterable<T> entities) {
        if (entities == null) {
            return new ArrayList<Key<T>>();
        }
        Iterator<T> iterator = entities.iterator();
        if (!iterator.hasNext()) {
            return new ArrayList<Key<T>>();
        }
        return save(entities, getWriteConcern(iterator.next()));
    }

    @Override
    public <T> Iterable<Key<T>> save(final Iterable<T> entities, final WriteConcern wc) {
        final List<Key<T>> savedKeys = new ArrayList<Key<T>>();
        for (final T ent : entities) {
            savedKeys.add(save(ent, wc));
        }
        return savedKeys;

    }

    @Override
    public <T> Iterable<Key<T>> save(final T... entities) {
        final List<Key<T>> savedKeys = new ArrayList<Key<T>>();
        for (final T ent : entities) {
            savedKeys.add(save(ent));
        }
        return savedKeys;
    }

    @Override
    public <T> Key<T> save(final T entity) {
        return save(entity, getWriteConcern(entity));
    }

    @Override
    public <T> Key<T> save(final T entity, final WriteConcern wc) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        return save(getCollection(unwrapped), unwrapped, wc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> UpdateResults update(final T entity, final UpdateOperations<T> operations) {
        if (entity instanceof Query) {
            return update((Query<T>) entity, operations);
        }

        final MappedClass mc = mapper.getMappedClass(entity);
        final Query<T> q = (Query<T>) createQuery(mc.getClazz());
        q.disableValidation().filter(Mapper.ID_KEY, mapper.getId(entity));

        if (!mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            final MappedField versionMF = mc.getFieldsAnnotatedWith(Version.class).get(0);
            final Long oldVer = (Long) versionMF.getFieldValue(entity);
            q.filter(versionMF.getNameToStore(), oldVer);
            operations.set(versionMF.getNameToStore(), nextValue(oldVer));
        }

        return update(q, operations);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> UpdateResults update(final Key<T> key, final UpdateOperations<T> operations) {
        Class<T> clazz = (Class<T>) key.getType();
        if (clazz == null) {
            clazz = (Class<T>) mapper.getClassFromCollection(key.getCollection());
        }
        return updateFirst(createQuery(clazz).disableValidation().filter(Mapper.ID_KEY, key.getId()), operations);
    }

    @Override
    public <T> UpdateResults update(final Query<T> query, final UpdateOperations<T> operations) {
        return update(query, operations, false, true);
    }

    @Override
    public <T> UpdateResults update(final Query<T> query, final UpdateOperations<T> operations, final boolean createIfMissing) {
        return update(query, operations, createIfMissing, getWriteConcern(query.getEntityClass()));
    }

    @Override
    public <T> UpdateResults update(final Query<T> query, final UpdateOperations<T> operations, final boolean createIfMissing,
                                    final WriteConcern wc) {
        return update(query, operations, createIfMissing, true, wc);
    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> operations) {
        return update(query, operations, false, false);
    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> operations, final boolean createIfMissing) {
        return update(query, operations, createIfMissing, false);

    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> operations, final boolean createIfMissing,
                                         final WriteConcern wc) {
        return update(query, operations, createIfMissing, false, wc);
    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final T entity, final boolean createIfMissing) {
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = mapper.toDBObject(entity, involvedObjects);

        final UpdateResults res = update(query, dbObj, createIfMissing, false, getWriteConcern(entity));

        // update _id field
        if (res.getInsertedCount() > 0) {
            dbObj.put(Mapper.ID_KEY, res.getNewId());
        }

        postSaveOperations(singletonList(entity), involvedObjects, getCollection(entity), false);
        return res;
    }

    @Override
    public <T> Query<T> createQuery(final String collection, final Class<T> type) {
        return newQuery(type, getDB().getCollection(collection));
    }

    @Override
    public <T> Query<T> createQuery(final Class<T> clazz, final DBObject q) {
        return newQuery(clazz, getCollection(clazz), q);
    }

    @Override
    public <T> Query<T> createQuery(final String collection, final Class<T> type, final DBObject q) {
        return newQuery(type, getCollection(collection), q);
    }

    @Override
    public <T, V> DBRef createRef(final Class<T> clazz, final V id) {
        if (id == null) {
            throw new MappingException("Could not get id for " + clazz.getName());
        }
        return new DBRef(getCollection(clazz).getName(), id);
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
    public <T> UpdateOperations<T> createUpdateOperations(final Class<T> type, final DBObject ops) {
        final UpdateOpsImpl<T> upOps = (UpdateOpsImpl<T>) createUpdateOperations(type);
        upOps.setOps(ops);
        return upOps;
    }

    @Override
    public <T, V> WriteResult delete(final String kind, final Class<T> clazz, final V id) {
        return delete(find(kind, clazz).filter(Mapper.ID_KEY, id));
    }

    @Override
    public <T, V> WriteResult delete(final String kind, final Class<T> clazz, final V id, final WriteConcern wc) {
        return delete(find(kind, clazz).filter(Mapper.ID_KEY, id), wc);
    }

    @Override
    @Deprecated
    public <T> void ensureIndex(final Class<T> type, final String fields) {
        ensureIndex(type, null, fields, false, false);
    }

    @Override
    @Deprecated
    public <T> void ensureIndex(final Class<T> clazz, final String name, final String fields, final boolean unique,
                                final boolean dropDupsOnCreate) {
        MappedClass mappedClass = getMapper().getMappedClass(clazz);
        ensureIndex(mappedClass.getCollectionName(), clazz, name, fields, unique, dropDupsOnCreate);
    }

    @Override
    public void ensureIndexes() {
        ensureIndexes(false);
    }

    @Override
    public void ensureIndexes(final boolean background) {
        for (final MappedClass mc : mapper.getMappedClasses()) {
            ensureIndexes(mc, background);
        }
    }

    @Override
    public <T> void ensureIndexes(final Class<T> clazz) {
        ensureIndexes(clazz, false);
    }

    @Override
    public <T> void ensureIndexes(final Class<T> clazz, final boolean background) {
        ensureIndexes(mapper.getMappedClass(clazz), background);
    }

    @Override
    public <T> void ensureIndex(final String collection, final Class<T> type, final String fields) {
        ensureIndex(collection, type, null, fields, false, false);
    }

    @Override
    public <T> void ensureIndex(final String collection, final Class<T> clazz, final String name, final String fields, final boolean unique,
                                final boolean dropDupsOnCreate) {
        if (dropDupsOnCreate) {
            throw new MappingException("dropDups value has been desupported by the server.  Please set this value to false and "
                                           + "validate your system behaves as expected.");
        }

        MappedClass mappedClass = getMapper().getMappedClass(clazz);
        Index index = synthesizeIndex(fields, name, false, false, false, unique, -1, null, null, null);
        ensureIndex(mappedClass, getMongoCollection(collection), index, false);
    }

    @Override
    public <T> void ensureIndexes(final String collection, final Class<T> clazz) {
        ensureIndexes(collection, clazz, false);
    }

    @Override
    public <T> void ensureIndexes(final String collection, final Class<T> clazz, final boolean background) {
        final MappedClass mc = mapper.getMappedClass(clazz);
        ensureIndexes(collection, mc, background);
    }

    protected void ensureIndexes(final String collName, final MappedClass mc, final boolean background) {
        ensureIndexes(getMongoCollection(collName), mc, background);
    }

    protected void ensureIndexes(final MappedClass mc, final boolean background) {
        ensureIndexes(getMongoCollection(mc.getClazz()), mc, background);
    }

    private void ensureIndex(final MappedClass mc, final MongoCollection collection, final Index index, final boolean background) {

        if (index.options().dropDups()) {
            throw new MappingException("dropDups value has been desupported by the server.  Please set this value to false and "
                                           + "validate your system behaves as expected.");
        }

        BsonDocument keys = calculateKeys(mc, index);

        createIndex(collection, keys, convert(index.options(), background));
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

    private String createIndex(final MongoCollection collection, final BsonDocument keys,
                               final com.mongodb.client.model.IndexOptions options) {
        return collection.createIndex(keys, options);
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

    private void ensureIndexes(final MongoCollection collection, final MappedClass mc, final boolean background) {

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

    private List<Index> collectIndexes(final MappedClass mc, final List<MappedClass> parentMCs) {
        if (parentMCs.contains(mc) || mc.getEmbeddedAnnotation() != null && parentMCs.isEmpty()) {
            return emptyList();
        }

        List<Index> indexes = collectTopLevelIndexes(mc);
        indexes.addAll(collectFieldIndexes(mc));
        indexes.addAll(collectNestedIndexes(mc, parentMCs));

        return indexes;
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
                        updated = synthesizeIndex(index);
                    }
                    List<Field> fields = new ArrayList<Field>();
                    for (Field field : updated.fields()) {
                        String path = findField(mc, index.options(), asList(field.value().split("\\.")));
                        fields.add(field(path, field.type(), field.weight()));
                    }

                    list.add(AnnotationUtils.replaceFields(updated, fields));
                }
            }
        }
        return list;
    }

    private List<Index> collectFieldIndexes(final MappedClass mc) {
        List<Index> list = new ArrayList<Index>();
        for (final MappedField mf : mc.getPersistenceFields()) {
            if (mf.hasAnnotation(Indexed.class)) {
                final Indexed index = mf.getAnnotation(Indexed.class);
                if (index.options().dropDups()) {
                    throw new MappingException("dropDups value has been desupported by the server.  Please set this value to false and "
                                                   + "validate your system behaves as expected.");
                }
                final BasicDBObject newOptions = (BasicDBObject) AnnotationUtils.extractOptions(index.options());
                if (!AnnotationUtils.extractOptions(index).isEmpty() && !newOptions.isEmpty()) {
                    throw new MappingException("Mixed usage of deprecated @Indexed value with the new @IndexOption values is not "
                                                   + "allowed.  Please migrate all settings to @IndexOptions");
                }

                List<Field> fields = singletonList(field(mf.getNameToStore(), fromValue(index.value().toIndexValue()), -1));
                list.add(!newOptions.isEmpty()
                         ? synthesizeIndex(index.name(), index.options().background(), true, index.options().sparse(),
                                           index.options().unique(), index.options().expireAfterSeconds(), index.options().language(),
                                           index.options().languageOverride(), index.options().collation(), fields)
                         : synthesizeIndex(index.name(), index.background(), true, index.sparse(), index.unique(),
                                           index.expireAfterSeconds(), null, null, null, fields));
            } else if (mf.hasAnnotation(Text.class)) {
                final Text index = mf.getAnnotation(Text.class);

                List<Field> fields = singletonList(field(mf.getNameToStore(), IndexType.TEXT, index.value()));
                list.add(synthesizeIndex(index.options().name(), index.options().background(), true, index.options().sparse(),
                                         index.options().unique(), index.options().expireAfterSeconds(), index.options().language(),
                                         index.options().languageOverride(), index.options().collation(), fields));
            }
        }
        return list;
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
                        fields.add(field(field.value().equals("$**")
                                         ? field.value()
                                         : mf.getNameToStore() + "." + field.value(),
                                         field.type(), field.weight()));
                    }
                    list.add(AnnotationUtils.replaceFields(index, fields));
                }
            }
        }

        return list;
    }

/*
    private void ensureIndexes(final MongoCollection collection, final MappedClass mc, final boolean background,
                               final List<MappedClass> parentMCs, final List<MappedField> parentMFs) {
        if (parentMCs.contains(mc) || mc.getEmbeddedAnnotation() != null && parentMCs.isEmpty()) {
            return;
        }

        processClassAnnotations(collection, mc, background);

        processEmbeddedAnnotations(collection, mc, background, parentMCs, parentMFs);
    }
*/

    @Override
    public Key<?> exists(final Object entityOrKey, final ReadPreference readPreference) {
        final Query<?> query = buildExistsQuery(entityOrKey);
        if (readPreference != null) {
            query.useReadPreference(readPreference);
        }
        return query.getKey();
    }

    @Override
    public <T> Query<T> find(final String collection, final Class<T> clazz) {
        return createQuery(collection, clazz);
    }

    @Override
    public <T, V> Query<T> find(final String collection, final Class<T> clazz, final String property, final V value, final int offset,
                                final int size) {
        return find(collection, clazz, property, value, offset, size, true);
    }

    @Override
    public <T> T get(final Class<T> clazz, final DBRef ref) {
        DBObject object = getDB().getCollection(ref.getCollectionName()).findOne(new BasicDBObject("_id", ref.getId()));
        return mapper.fromDBObject(this, clazz, object, createCache());
    }

    @Override
    public <T, V> T get(final String collection, final Class<T> clazz, final V id) {
        final List<T> results = find(collection, clazz, Mapper.ID_KEY, id, 0, 1).asList();
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    @Override
    public long getCount(final String collection) {
        return getCollection(collection).count();
    }

    @Override
    public DBDecoderFactory getDecoderFact() {
        return decoderFactory != null ? decoderFactory : DefaultDBDecoder.FACTORY;
    }

    @Override
    public void setDecoderFact(final DBDecoderFactory fact) {
        decoderFactory = fact;
    }

    @Override
    public <T> Key<T> insert(final String collection, final T entity) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        final DBCollection dbColl = getCollection(collection);
        return insert(dbColl, unwrapped, getWriteConcern(unwrapped));
    }

    @Override
    public <T> Key<T> insert(final T entity) {
        return insert(entity, getWriteConcern(entity));
    }

    @Override
    public <T> Key<T> insert(final T entity, final WriteConcern wc) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        final DBCollection dbColl = getCollection(unwrapped);
        return insert(dbColl, unwrapped, wc);
    }

    @Override
    public <T> Iterable<Key<T>> insert(final T... entities) {
        if (entities.length == 0) {
            return Collections.emptyList();
        } else {
            return insert(asList(entities), getWriteConcern(entities[0]));
        }
    }

    @Override
    public <T> Iterable<Key<T>> insert(final Iterable<T> entities, final WriteConcern wc) {
        if (!entities.iterator().hasNext()) {
            return Collections.emptyList();
        } else {
            return insert(getCollection(entities.iterator().next()), entities, wc);
        }
    }

    @Override
    public <T> Iterable<Key<T>> insert(final String collection, final Iterable<T> entities) {
        return insert(collection, entities, null);
    }

    @Override
    public <T> Iterable<Key<T>> insert(final String collection, final Iterable<T> entities, final WriteConcern wc) {
        return insert(getDB().getCollection(collection), entities, wc);
    }

    @Override
    public <T> Query<T> queryByExample(final String collection, final T ex) {
        return queryByExample(getDB().getCollection(collection), ex);
    }

    @Override
    public <T> Key<T> save(final String collection, final T entity) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        return save(collection, entity, getWriteConcern(unwrapped));
    }

    @Override
    public <T> Key<T> save(final String collection, final T entity, final WriteConcern wc) {
        return save(getCollection(collection), ProxyHelper.unwrap(entity), wc);
    }

    /**
     * Deletes entities based on the query, with the WriteConcern
     *
     * @param clazz the clazz to query against when finding documents to delete
     * @param id    the ID to look for
     * @param wc    the WriteConcern to use when deleting
     * @param <T>   the type to delete
     * @param <V>   the type of the key
     * @return results of the delete
     */
    public <T, V> WriteResult delete(final Class<T> clazz, final V id, final WriteConcern wc) {
        return delete(createQuery(clazz).filter(Mapper.ID_KEY, id), wc);
    }

    /**
     * Find all instances by type in a different collection than what is mapped on the class given skipping some documents and returning a
     * fixed number of the remaining.
     *
     * @param collection The collection use when querying
     * @param clazz      the class to use for mapping the results
     * @param property   the document property to query against
     * @param value      the value to check for
     * @param offset     the number of results to skip
     * @param size       the maximum number of results to return
     * @param validate   if true, validate the query
     * @param <T>        the type to query
     * @param <V>        the type to filter value
     * @return the query
     */
    public <T, V> Query<T> find(final String collection, final Class<T> clazz, final String property, final V value, final int offset,
                                final int size, final boolean validate) {
        final Query<T> query = find(collection, clazz);
        if (!validate) {
            query.disableValidation();
        }
        query.offset(offset);
        query.limit(size);
        return query.filter(property, value).enableValidation();
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
     * Inserts entities in to the database
     *
     * @param entities the entities to insert
     * @param <T>      the type of the entities
     * @return the keys of entities
     */
    @Override
    public <T> Iterable<Key<T>> insert(final Iterable<T> entities) {
        return insert(entities, null);
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
        final T unwrapped = ProxyHelper.unwrap(entity);
        final DBCollection dbColl = getCollection(collection);
        return insert(dbColl, unwrapped, wc);
    }

    protected DBCollection getCollection(final String kind) {
        if (kind == null) {
            return null;
        }
        return getDB().getCollection(kind);
    }

    @Deprecated
    protected Object getId(final Object entity) {
        return mapper.getId(entity);
    }

    protected <T> Key<T> insert(final DBCollection dbColl, final T entity, final WriteConcern wc) {
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = entityToDBObj(entity, involvedObjects);
        if (wc == null) {
            dbColl.insert(dbObj);
        } else {
            dbColl.insert(dbObj, wc);
        }

        return postSaveOperations(singletonList(entity), involvedObjects, dbColl).get(0);
    }

    protected <T> Key<T> save(final DBCollection dbColl, final T entity, final WriteConcern wc) {
        if (entity == null) {
            throw new UpdateException("Can not persist a null entity");
        }

        final MappedClass mc = mapper.getMappedClass(entity);
        if (mc.getAnnotation(NotSaved.class) != null) {
            throw new MappingException(format("Entity type: %s is marked as NotSaved which means you should not try to save it!",
                                              mc.getClazz().getName()));
        }

        // involvedObjects is used not only as a cache but also as a list of what needs to be called for life-cycle methods at the end.
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = entityToDBObj(entity, involvedObjects);

        // try to do an update if there is a @Version field
        final Object idValue = dbObj.get(Mapper.ID_KEY);
        WriteResult wr = tryVersionedUpdate(dbColl, entity, dbObj, idValue, wc, mc);

        if (wr == null) {
            if (wc == null) {
                dbColl.save(dbObj);
            } else {
                dbColl.save(dbObj, wc);
            }
        }

        return postSaveOperations(singletonList(entity), involvedObjects, dbColl).get(0);
    }

    private <T> WriteResult tryVersionedUpdate(final DBCollection dbColl, final T entity, final DBObject dbObj, final Object idValue,
                                                 final WriteConcern wc, final MappedClass mc) {
        WriteResult wr;
        if (mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            return null;
        }

        final MappedField mfVersion = mc.getMappedVersionField();
        final String versionKeyName = mfVersion.getNameToStore();

        Long oldVersion = (Long) mfVersion.getFieldValue(entity);
        long newVersion = nextValue(oldVersion);

        dbObj.put(versionKeyName, newVersion);
        //        mfVersion.setFieldValue(entity, newVersion);

        if (idValue != null && newVersion != 1) {
            final Query<?> query = find(dbColl.getName(), entity.getClass());
            boolean compoundId = !ReflectionUtils.isPrimitiveLike(mc.getMappedIdField().getType())
                                     && idValue instanceof DBObject;
            if (compoundId) {
                query.disableValidation();
            }
            query.filter(Mapper.ID_KEY, idValue);
            if (compoundId) {
                query.enableValidation();
            }
            query.filter(versionKeyName, oldVersion);
            final UpdateResults res = update(query, dbObj, false, false, wc);

            wr = res.getWriteResult();

            if (res.getUpdatedCount() != 1) {
                throw new ConcurrentModificationException(format("Entity of class %s (id='%s',version='%d') was concurrently updated.",
                                                                 entity.getClass().getName(), idValue, oldVersion));
            }
        } else {
            if (wc == null) {
                wr = dbColl.save(dbObj);
            } else {
                wr = dbColl.save(dbObj, wc);
            }
        }

        return wr;
    }

    private Query<?> buildExistsQuery(final Object entityOrKey) {
        final Object unwrapped = ProxyHelper.unwrap(entityOrKey);
        final Key<?> key = mapper.getKey(unwrapped);
        final Object id = key.getId();
        if (id == null) {
            throw new MappingException("Could not get id for " + unwrapped.getClass().getName());
        }

        return find(key.getCollection(), key.getType()).filter(Mapper.ID_KEY, key.getId());
    }

    private EntityCache createCache() {
        return mapper.createEntityCache();
    }

    private DBObject entityToDBObj(final Object entity, final Map<Object, DBObject> involvedObjects) {
        return mapper.toDBObject(ProxyHelper.unwrap(entity), involvedObjects);
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
                namePath += "." + findField(getMapper().getMappedClass(concreteType), options, path.subList(1, path.size()));
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

    @SuppressWarnings("unchecked")
    private <T> Iterable<Key<T>> insert(final DBCollection dbColl, final Iterable<T> entities, final WriteConcern wc) {
        if (!entities.iterator().hasNext()) {
            return Collections.emptyList();
        }

        WriteConcern writeConcern = wc;
        if (writeConcern == null) {
            writeConcern = getWriteConcern(entities.iterator().next());
        }
        final Map<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        if (morphia.getUseBulkWriteOperations()) {
            BulkWriteOperation bulkWriteOperation = dbColl.initializeOrderedBulkOperation();
            for (final T entity : entities) {
                bulkWriteOperation.insert(toDbObject(entity, involvedObjects));
            }
            bulkWriteOperation.execute(writeConcern);
        } else {
            writeConcern = getWriteConcern(entities.iterator().next());
            final List<DBObject> list = new ArrayList<DBObject>();
            for (final T entity : entities) {
                list.add(toDbObject(entity, involvedObjects));
            }
            dbColl.insert(writeConcern, list.toArray(new DBObject[list.size()]));
        }

        return postSaveOperations(entities, involvedObjects, dbColl);
    }

    /**
     * Creates and returns a {@link Query} using the underlying {@link QueryFactory}.
     *
     * @see QueryFactory#createQuery(Datastore, DBCollection, Class, DBObject)
     */
    private <T> Query<T> newQuery(final Class<T> type, final DBCollection collection, final DBObject query) {
        return getQueryFactory().createQuery(this, collection, type, query);
    }

    /**
     * Creates and returns a {@link Query} using the underlying {@link QueryFactory}.
     *
     * @see QueryFactory#createQuery(Datastore, DBCollection, Class)
     */
    private <T> Query<T> newQuery(final Class<T> type, final DBCollection collection) {
        return getQueryFactory().createQuery(this, collection, type);
    }

    private long nextValue(final Long oldVersion) {
        return oldVersion == null ? 1 : oldVersion + 1;
    }

    private <T> List<Key<T>> postSaveOperations(final Iterable<T> entities, final Map<Object, DBObject> involvedObjects,
                                                final DBCollection collection) {
        return postSaveOperations(entities, involvedObjects, collection, true);
    }

    @SuppressWarnings("unchecked")
    private <T> List<Key<T>> postSaveOperations(final Iterable<T> entities, final Map<Object, DBObject> involvedObjects,
                                                final DBCollection collection, final boolean fetchKeys) {
        List<Key<T>> keys = new ArrayList<Key<T>>();
        for (final T entity : entities) {
            final DBObject dbObj = involvedObjects.remove(entity);

            if (fetchKeys) {
                if (dbObj.get(Mapper.ID_KEY) == null) {
                    throw new MappingException(format("Missing _id after save on %s", entity.getClass().getName()));
                }
                mapper.updateKeyAndVersionInfo(this, dbObj, createCache(), entity);
                keys.add(new Key<T>((Class<? extends T>) entity.getClass(), collection.getName(), mapper.getId(entity)));
            }
            mapper.getMappedClass(entity).callLifecycleMethods(PostPersist.class, entity, dbObj, mapper);
        }

        for (Entry<Object, DBObject> entry : involvedObjects.entrySet()) {
            final Object key = entry.getKey();
            mapper.getMappedClass(key).callLifecycleMethods(PostPersist.class, key, entry.getValue(), mapper);

        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private <T> Query<T> queryByExample(final DBCollection coll, final T example) {
        // TODO: think about remove className from baseQuery param below.
        final Class<T> type = (Class<T>) example.getClass();
        final DBObject query = entityToDBObj(example, new HashMap<Object, DBObject>());
        return newQuery(type, coll, query);
    }

    private <T> DBObject toDbObject(final T ent, final Map<Object, DBObject> involvedObjects) {
        final MappedClass mc = mapper.getMappedClass(ent);
        if (mc.getAnnotation(NotSaved.class) != null) {
            throw new MappingException(format("Entity type: %s is marked as NotSaved which means you should not try to save it!",
                                              mc.getClazz().getName()));
        }
        DBObject dbObject = entityToDBObj(ent, involvedObjects);
        List<MappedField> versionFields = mc.getFieldsAnnotatedWith(Version.class);
        for (MappedField mappedField : versionFields) {
            String name = mappedField.getNameToStore();
            if (dbObject.get(name) == null) {
                dbObject.put(name, 1);
                mappedField.setFieldValue(ent, 1L);
            }
        }
        return dbObject;
    }

    private <T> UpdateResults update(final Query<T> query, final UpdateOperations ops, final boolean createIfMissing, final boolean multi) {
        return update(query, ops, createIfMissing, multi, getWriteConcern(query.getEntityClass()));
    }

    @SuppressWarnings("rawtypes")
    private <T> UpdateResults update(final Query<T> query, final UpdateOperations ops, final boolean createIfMissing, final boolean multi,
                                     final WriteConcern wc) {
        final DBObject u = ((UpdateOpsImpl) ops).getOps();
        if (((UpdateOpsImpl) ops).isIsolated()) {
            final Query<T> q = query.cloneQuery();
            q.disableValidation().filter("$atomic", true);
            return update(q, u, createIfMissing, multi, wc);
        }
        return update(query, u, createIfMissing, multi, wc);
    }

    @SuppressWarnings("unchecked")
    private <T> UpdateResults update(final Query<T> query, final DBObject update, final boolean createIfMissing, final boolean multi,
                                     final WriteConcern wc) {

        DBCollection dbColl = query.getCollection();
        // TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(query.getEntityClass());
        }

        if (query.getSortObject() != null && query.getSortObject().keySet() != null && !query.getSortObject().keySet().isEmpty()) {
            throw new QueryException("sorting is not allowed for updates.");
        }
        if (query.getOffset() > 0) {
            throw new QueryException("a query offset is not allowed for updates.");
        }
        if (query.getLimit() > 0) {
            throw new QueryException("a query limit is not allowed for updates.");
        }

        DBObject queryObject = query.getQueryObject();
        if (queryObject == null) {
            queryObject = new BasicDBObject();
        }

        final MappedClass mc = getMapper().getMappedClass(query.getEntityClass());
        final List<MappedField> fields = mc.getFieldsAnnotatedWith(Version.class);
        if (!fields.isEmpty()) {
            final MappedField versionMF = fields.get(0);
            if (queryObject.get(versionMF.getNameToStore()) == null) {
                if (!update.containsField("$inc")) {
                    update.put("$inc", new BasicDBObject(versionMF.getNameToStore(), 1));
                } else {
                    ((Map<String, Object>) (update.get("$inc"))).put(versionMF.getNameToStore(), 1);
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Executing update(%s) for query: %s, ops: %s, multi: %s, upsert: %s",
                             dbColl.getName(), queryObject, update, multi, createIfMissing));
        }

        final WriteResult wr;
        if (wc == null) {
            wr = dbColl.update(queryObject, update, createIfMissing, multi);
        } else {
            wr = dbColl.update(queryObject, update, createIfMissing, multi, wc);
        }

        return new UpdateResults(wr);
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazzOrEntity the class or entity to use when looking up the WriteConcern
     */
    private WriteConcern getWriteConcern(final Object clazzOrEntity) {
        WriteConcern wc = defConcern;
        if (clazzOrEntity != null) {
            final Entity entityAnn = getMapper().getMappedClass(clazzOrEntity).getEntityAnnotation();
            if (entityAnn != null && entityAnn.concern().length() != 0) {
                wc = WriteConcern.valueOf(entityAnn.concern());
            }
        }

        return wc;
    }
}
