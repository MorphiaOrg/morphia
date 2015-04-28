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
import org.mongodb.morphia.annotations.Transient;
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
import org.mongodb.morphia.query.QueryImpl;
import org.mongodb.morphia.query.UpdateException;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateOpsImpl;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.utils.Assert;
import org.mongodb.morphia.utils.IndexType;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * A generic (type-safe) wrapper around mongodb collections
 *
 * @author Scott Hernandez
 */
public class DatastoreImpl implements AdvancedDatastore {
    private static final Logger LOG = MorphiaLoggerFactory.get(DatastoreImpl.class);

    private final Morphia morphia;
    private final Mapper mapper;
    private final MongoClient mongoClient;
    private final DB db;
    private WriteConcern defConcern = WriteConcern.SAFE;
    private DBDecoderFactory decoderFactory;

    private volatile QueryFactory queryFactory = new DefaultQueryFactory();

    /**
     * Create a new DatastoreImpl
     *
     * @param morphia     the Morphia instance
     * @param mapper      an initialised Mapper
     * @param mongoClient the connection to the MongoDB instance
     * @param dbName      the name of the database for this data store.
     */
    public DatastoreImpl(final Morphia morphia, final Mapper mapper, final MongoClient mongoClient, final String dbName) {
        this.morphia = morphia;
        this.mapper = mapper;
        this.mongoClient = mongoClient;
        db = mongoClient.getDB(dbName);

        // VERY discussable
        mapper.getDatastoreProvider().register(this);
    }

    /**
     * Create a new DatastoreImpl
     *
     * @param morphia     the Morphia instance
     * @param mongoClient the connection to the MongoDB instance
     * @param dbName      the name of the database for this data store.
     */
    public DatastoreImpl(final Morphia morphia, final MongoClient mongoClient, final String dbName) {
        this(morphia, morphia.getMapper(), mongoClient, dbName);
    }

    public static long nextValue(final Long oldVersion) {
        return oldVersion == null ? 1 : oldVersion + 1;
    }

    public DatastoreImpl copy(final String database) {
        return new DatastoreImpl(morphia, mapper, mongoClient, database);
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

    @Deprecated
    protected Object getId(final Object entity) {
        return mapper.getId(entity);
    }

    @Override
    @Deprecated // use mapper instead.
    public <T> Key<T> getKey(final T entity) {
        return mapper.getKey(entity);
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
    public <T, V> WriteResult delete(final Class<T> clazz, final V id) {
        return delete(clazz, id, getWriteConcern(clazz));
    }

    public <T, V> WriteResult delete(final Class<T> clazz, final V id, final WriteConcern wc) {
        return delete(createQuery(clazz).filter(Mapper.ID_KEY, id), wc);
    }

    @Override
    public <T, V> WriteResult delete(final Class<T> clazz, final Iterable<V> ids) {
        final Query<T> q = find(clazz).filter(Mapper.ID_KEY + " in", ids);
        return delete(q);
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
    public <T> WriteResult delete(final Query<T> query) {
        return delete(query, getWriteConcern(query.getEntityClass()));
    }

    @Override
    public <T> WriteResult delete(final Query<T> query, final WriteConcern wc) {

        DBCollection dbColl = query.getCollection();
        //TODO remove this after testing.
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
    public <T> void ensureIndex(final Class<T> type, final String fields) {
        ensureIndex(type, null, fields, false, false);
    }

    @Override
    public <T> void ensureIndex(final Class<T> clazz, final String name, final String fields, final boolean unique,
                                final boolean dropDupsOnCreate) {
        ensureIndex(clazz, name, QueryImpl.parseFieldsString(fields, clazz, mapper, true), unique, dropDupsOnCreate, false, false, -1);
    }

    protected <T> void ensureIndex(final Class<T> clazz, final String name, final BasicDBObject fields, final boolean unique,
                                   final boolean dropDupsOnCreate, final boolean background, final boolean sparse,
                                   final int expireAfterSeconds) {
        final DBCollection dbColl = getCollection(clazz);
        ensureIndex(dbColl, name, fields, unique, dropDupsOnCreate,
                    background, sparse, expireAfterSeconds);
    }

    @Override
    public <T> void ensureIndex(final String collName, final Class<T> type,
                                final String fields) {
        ensureIndex(collName, type, null, fields, false, false);
    }

    @Override
    public <T> void ensureIndex(final String collName, final Class<T> clazz,
                                final String name, final String fields, final boolean unique,
                                final boolean dropDupsOnCreate) {
        ensureIndex(getCollection(collName), name,
                    QueryImpl.parseFieldsString(fields, clazz, mapper, true), unique,
                    dropDupsOnCreate, false, false, -1);
    }

    protected void ensureIndex(final DBCollection dbColl,
                               final String name, final BasicDBObject fields,
                               final boolean unique, final boolean dropDupsOnCreate,
                               final boolean background, final boolean sparse,
                               final int expireAfterSeconds) {
        final BasicDBObject opts = new BasicDBObject();
        if (name != null && name.length() != 0) {
            opts.append("name", name);
        }
        if (unique) {
            opts.append("unique", true);
            if (dropDupsOnCreate) {
                opts.append("dropDups", true);
            }
        }

        if (background) {
            opts.append("background", true);
        }
        if (sparse) {
            opts.append("sparse", true);
        }

        if (expireAfterSeconds > -1) {
            opts.append("expireAfterSeconds", expireAfterSeconds);
        }

        LOG.debug(format("Creating index for %s with keys:%s and opts:%s", dbColl.getName(), fields, opts));
        dbColl.createIndex(fields, opts);
    }

    protected void ensureIndexes(final MappedClass mc, final boolean background) {
        ensureIndexes(mc, background, new ArrayList<MappedClass>(), new ArrayList<MappedField>());
    }

    protected void ensureIndexes(final MappedClass mc, final boolean background, final List<MappedClass> parentMCs,
                                 final List<MappedField> parentMFs) {
        ensureIndexes(getCollection(mc.getClazz()), mc, background, parentMCs, parentMFs);
    }

    protected void ensureIndexes(final String collName, final MappedClass mc, final boolean background) {
        ensureIndexes(getCollection(collName), mc, background, new ArrayList<MappedClass>(),
                      new ArrayList<MappedField>());
    }

    protected void ensureIndexes(final DBCollection dbColl, final MappedClass mc, final boolean background,
                                 final List<MappedClass> parentMCs, final List<MappedField> parentMFs) {
        if (parentMCs.contains(mc)) {
            return;
        }

        if (mc.getEmbeddedAnnotation() != null && parentMCs.isEmpty()) {
            return;
        }
        processClassAnnotations(dbColl, mc, background);

        processEmbeddedAnnotations(dbColl, mc, background, parentMCs, parentMFs);
    }

    /**
     * Ensure indexes from field annotations, and embedded entities
     */
    private void processEmbeddedAnnotations(final DBCollection dbColl, final MappedClass mc, final boolean background,
                                            final List<MappedClass> parentMCs, final List<MappedField> parentMFs) {
        List<MappedField> annotatedWith = mc.getFieldsAnnotatedWith(Text.class);
        if (annotatedWith.size() > 1) {
            throw new MappingException("Only one text index can be defined per collection: " + mc.getClazz().getName());
        }
        for (final MappedField mf : mc.getPersistenceFields()) {
            if (mf.hasAnnotation(Indexed.class)) {
                final Indexed index = mf.getAnnotation(Indexed.class);
                final StringBuilder field = new StringBuilder();
                if (!parentMCs.isEmpty()) {
                    for (final MappedField pmf : parentMFs) {
                        field.append(pmf.getNameToStore()).append(".");
                    }
                }

                field.append(mf.getNameToStore());

                ensureIndex(dbColl,
                            index.name(),
                            new BasicDBObject(field.toString(), index.value().toIndexValue()),
                            index.unique(),
                            index.dropDups(),
                            index.background() ? index.background() : background,
                            index.sparse(),
                            index.expireAfterSeconds());
            }

            if (mf.hasAnnotation(Text.class)) {
                createTextIndex(dbColl, parentMCs, parentMFs, mf);
            }

            if (!mf.isTypeMongoCompatible() && !mf.hasAnnotation(Reference.class) && !mf.hasAnnotation(Serialized.class)
                && !mf.hasAnnotation(NotSaved.class) && !mf.hasAnnotation(Transient.class)) {
                final List<MappedClass> newParentClasses = new ArrayList<MappedClass>(parentMCs);
                final List<MappedField> newParents = new ArrayList<MappedField>(parentMFs);
                newParentClasses.add(mc);
                newParents.add(mf);
                ensureIndexes(dbColl,
                              mapper.getMappedClass(mf.isSingleValue() ? mf.getType() : mf.getSubClass()),
                              background,
                              newParentClasses,
                              newParents);
            }
        }
    }

    protected void ensureIndex(final MappedClass mc, final DBCollection dbColl, final Field[] fields, final IndexOptions options) {
        DBObject keys = new BasicDBObject();
        DBObject opts = extractOptions(options);
        for (Field field : fields) {
            String value = field.value();
            String key = value;
            if (!"$**".equals(value)) {
                ArrayList<String> namePath = new ArrayList<String>();
                MappedField mf = findField(namePath, mc, value);
                if (mf == null) {
                    throw new MappingException(format("Unknown field '%s' for index: %s", value, mc.getClazz().getName()));
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String s : namePath) {
                        if (sb.length() != 0) {
                            sb.append(".");
                        }
                        sb.append(s);
                    }
                    key = sb.toString();
                }
            }
            keys.put(key, field.type().toIndexValue());
            if (field.weight() != -1) {
                if (field.type() != IndexType.TEXT) {
                    throw new MappingException("Weight values only apply to text indexes: " + Arrays.toString(fields));
                }
                DBObject weights = (DBObject) opts.get("weights");
                if (weights == null) {
                    weights = new BasicDBObject();
                    opts.put("weights", weights);
                }
                weights.put(key, field.weight());
            }
        }

        LOG.debug(format("Creating index for %s with keys:%s and opts:%s", dbColl.getName(), keys, opts));
        dbColl.createIndex(keys, opts);
    }

    private MappedField findField(final List<String> namePath, final MappedClass mc, final String value) {
        if (value.contains(".")) {
            String segment = value.substring(0, value.indexOf("."));
            MappedField field = findField(namePath, mc, segment);
            MappedClass mappedClass = getMapper().getMappedClass(field.getSubType() != null
                                                                 ? field.getSubType() 
                                                                 : field.getConcreteType());
            return findField(namePath, mappedClass, value.substring(value.indexOf(".") + 1));
        } else {
            MappedField mf = mc.getMappedField(value);
            if (mf == null) {
                mf = mc.getMappedFieldByJavaField(value);
            }
            if (mf != null) {
                namePath.add(mf.getNameToStore());
            }
            return mf;
        }
    }

    private void createTextIndex(final DBCollection dbColl, final List<MappedClass> parentMCs, final List<MappedField> parentMFs,
                                 final MappedField mf) {
        final Text index = mf.getAnnotation(Text.class);
        final StringBuilder field = new StringBuilder();
        if (!parentMCs.isEmpty()) {
            for (final MappedField pmf : parentMFs) {
                field.append(pmf.getNameToStore()).append(".");
            }
        }

        field.append(mf.getNameToStore());

        DBObject keys = new BasicDBObject(field.toString(), IndexType.TEXT.toIndexValue());
        DBObject opts = extractOptions(index.options());
        if (index.value() != -1) {
            DBObject weights = new BasicDBObject();
            opts.put("weights", weights);
            weights.put(field.toString(), index.value());
        }
        LOG.debug(format("Creating index for %s with keys:%s and opts:%s", dbColl.getName(), keys, opts));
        dbColl.createIndex(keys, opts);
    }

    @SuppressWarnings("deprecation")
    private void processClassAnnotations(final DBCollection dbColl, final MappedClass mc, final boolean background) {
        //Ensure indexes from class annotation
        final List<Annotation> indexes = mc.getAnnotations(Indexes.class);
        if (indexes != null) {
            for (final Annotation ann : indexes) {
                final Indexes idx = (Indexes) ann;
                if (idx.value().length > 0) {
                    for (final Index index : idx.value()) {
                        if (index.fields().length != 0) {
                            ensureIndex(mc, dbColl, index.fields(), index.options());
                        } else {
                            LOG.warning(format("This index on '%s' is using deprecated configuration options.  Please update to use the "
                                               + "fields value on @Index: %s", mc.getClazz().getName(), index.toString()));
                            final BasicDBObject fields = QueryImpl.parseFieldsString(index.value(),
                                                                                     mc.getClazz(),
                                                                                     mapper,
                                                                                     !index.disableValidation());
                            ensureIndex(dbColl, index.name(), fields, index.unique(), index.dropDups(),
                                        index.background() ? index.background() : background, index.sparse(), index.expireAfterSeconds());
                        }
                    }
                }
            }
        }
    }

    @Override
    public <T> void ensureIndexes(final Class<T> clazz) {
        ensureIndexes(clazz, false);
    }

    @Override
    public <T> void ensureIndexes(final Class<T> clazz, final boolean background) {
        final MappedClass mc = mapper.getMappedClass(clazz);
        ensureIndexes(mc, background);
    }

    @Override
    public void ensureIndexes() {
        ensureIndexes(false);
    }

    @Override
    public void ensureIndexes(final boolean background) {
        // loops over mappedClasses and call ensureIndex for each @Entity object
        // (for now)
        for (final MappedClass mc : mapper.getMappedClasses()) {
            ensureIndexes(mc, background);
        }
    }

    @Override
    public <T> void ensureIndexes(final String collName, final Class<T> clazz) {
        ensureIndexes(collName, clazz, false);
    }

    @Override
    public <T> void ensureIndexes(final String collName, final Class<T> clazz, final boolean background) {
        final MappedClass mc = mapper.getMappedClass(clazz);
        ensureIndexes(collName, mc, background);
    }

    private DBObject extractOptions(final IndexOptions options) {
        final DBObject opts = new BasicDBObject();

        putIfNotEmpty(opts, "name", options.name());
        putIfNotEmpty(opts, "default_language", options.language());
        putIfNotEmpty(opts, "language_override", options.languageOverride());
        putIfTrue(opts, "background", options.background());
        putIfTrue(opts, "dropDups", options.dropDups());
        putIfTrue(opts, "sparse", options.sparse());
        putIfTrue(opts, "unique", options.unique());
        if (options.expireAfterSeconds() != -1) {
            opts.put("expireAfterSeconds", options.expireAfterSeconds());
        }
        return opts;
    }

    private void putIfNotEmpty(final DBObject opts, final String key, final String value) {
        if (!value.equals("")) {
            opts.put(key, value);
        }
    }

    private void putIfTrue(final DBObject opts, final String key, final boolean value) {
        if (value) {
            opts.put(key, true);
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

    /**
     * Creates and returns a {@link Query} using the underlying {@link QueryFactory}.
     *
     * @see QueryFactory#createQuery(Datastore, DBCollection, Class)
     */
    private <T> Query<T> newQuery(final Class<T> type, final DBCollection collection) {
        return getQueryFactory().createQuery(this, collection, type);
    }

    /**
     * Creates and returns a {@link Query} using the underlying {@link QueryFactory}.
     *
     * @see QueryFactory#createQuery(Datastore, DBCollection, Class, DBObject)
     */
    private <T> Query<T> newQuery(final Class<T> type, final DBCollection collection, final DBObject query) {
        return getQueryFactory().createQuery(this, collection, type, query);
    }

    @Override
    public <T> Query<T> queryByExample(final T ex) {
        return queryByExample(getCollection(ex), ex);
    }

    @Override
    public <T> Query<T> queryByExample(final String kind, final T ex) {
        return queryByExample(db.getCollection(kind), ex);
    }

    @SuppressWarnings("unchecked")
    private <T> Query<T> queryByExample(final DBCollection coll, final T example) {
        //TODO: think about remove className from baseQuery param below.
        final Class<T> type = (Class<T>) example.getClass();
        final DBObject query = entityToDBObj(example, new HashMap<Object, DBObject>());
        return newQuery(type, coll, query);
    }

    /**
     * Returns a new query bound to the kind (a specific {@link DBCollection})
     */
    @Override
    public AggregationPipeline createAggregation(final Class source) {
        return new AggregationPipelineImpl(this, source);
    }

    @Override
    public <T> Query<T> createQuery(final Class<T> type) {
        return newQuery(type, getCollection(type));
    }

    @Override
    public <T> Query<T> createQuery(final String kind, final Class<T> type) {
        return newQuery(type, db.getCollection(kind));
    }

    @Override
    public <T> Query<T> createQuery(final Class<T> type, final DBObject q) {
        return newQuery(type, getCollection(type), q);
    }

    @Override
    public <T> Query<T> createQuery(final String kind, final Class<T> type, final DBObject q) {
        return newQuery(type, getCollection(kind), q);
    }

    @Override
    public <T> Query<T> find(final String kind, final Class<T> clazz) {
        return createQuery(kind, clazz);
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
    public <T, V> Query<T> find(final String kind, final Class<T> clazz, final String property, final V value, final int offset,
                                final int size) {
        return find(kind, clazz, property, value, offset, size, true);
    }

    public <T, V> Query<T> find(final String kind, final Class<T> clazz, final String property, final V value, final int offset,
                                final int size, final boolean validate) {
        final Query<T> query = find(kind, clazz);
        if (!validate) {
            query.disableValidation();
        }
        query.offset(offset);
        query.limit(size);
        return query.filter(property, value).enableValidation();
    }


    @Override
    public <T, V> Query<T> find(final Class<T> clazz, final String property, final V value, final int offset, final int size) {
        final Query<T> query = createQuery(clazz);
        query.offset(offset);
        query.limit(size);
        return query.filter(property, value);
    }


    @Override
    public <T> T get(final Class<T> clazz, final DBRef ref) {
        DBObject object = getDB().getCollection(ref.getCollectionName()).findOne(new BasicDBObject("_id", ref.getId()));
        return mapper.fromDBObject(clazz, object, createCache());
    }


    @Override
    public <T, V> Query<T> get(final Class<T> clazz, final Iterable<V> ids) {
        return find(clazz).disableValidation().filter(Mapper.ID_KEY + " in", ids).enableValidation();
    }

    @Override
    public <T> List<T> getByKeys(final Iterable<Key<T>> keys) {
        return getByKeys(null, keys);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> List<T> getByKeys(final Class<T> clazz, final Iterable<Key<T>> keys) {

        final Map<String, List<Key>> kindMap = new HashMap<String, List<Key>>();
        final List<T> entities = new ArrayList<T>();
        // String clazzKind = (clazz==null) ? null :
        // getMapper().getCollectionName(clazz);
        for (final Key<?> key : keys) {
            mapper.updateKind(key);

            // if (clazzKind != null && !key.getKind().equals(clazzKind))
            // throw new IllegalArgumentException("Types are not equal (" +
            // clazz + "!=" + key.getKindClass() +
            // ") for key and method parameter clazz");
            //
            if (kindMap.containsKey(key.getCollection())) {
                kindMap.get(key.getCollection()).add(key);
            } else {
                kindMap.put(key.getCollection(), new ArrayList<Key>(Collections.singletonList((Key) key)));
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

        //TODO: order them based on the incoming Keys.
        return entities;
    }


    @Override
    public <T, V> T get(final String kind, final Class<T> clazz, final V id) {
        final List<T> results = find(kind, clazz, Mapper.ID_KEY, id, 0, 1).asList();
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }


    @Override
    public <T, V> T get(final Class<T> clazz, final V id) {
        return find(getCollection(clazz).getName(), clazz, Mapper.ID_KEY, id, 0, 1, true).get();
    }


    @Override
    public <T> T getByKey(final Class<T> clazz, final Key<T> key) {
        final String kind = mapper.getCollectionName(clazz);
        final String keyKind = mapper.updateKind(key);
        if (!kind.equals(keyKind)) {
            throw new RuntimeException("collection names don't match for key and class: " + kind + " != " + keyKind);
        }

        return get(clazz, key.getId());
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
    public Key<?> exists(final Object entityOrKey) {
        final Query<?> query = buildExistsQuery(entityOrKey);
        return query.getKey();
    }

    @Override
    public Key<?> exists(final Object entityOrKey, final ReadPreference readPreference) {
        final Query<?> query = buildExistsQuery(entityOrKey);
        if (readPreference != null) {
            query.useReadPreference(readPreference);
        }
        return query.getKey();
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

    @Override
    public DBCollection getCollection(final Class clazz) {
        final String collName = mapper.getCollectionName(clazz);
        return getDB().getCollection(collName);
    }

    public DBCollection getCollection(final Object obj) {
        if (obj == null) {
            return null;
        }
        return getCollection(obj.getClass());
    }

    protected DBCollection getCollection(final String kind) {
        if (kind == null) {
            return null;
        }
        return getDB().getCollection(kind);
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
    public long getCount(final String kind) {
        return getCollection(kind).count();
    }


    @Override
    public <T> long getCount(final Query<T> query) {
        return query.countAll();
    }

    @Override
    public MongoClient getMongo() {
        return mongoClient;
    }

    @Override
    public DB getDB() {
        return db;
    }

    public Mapper getMapper() {
        return mapper;
    }

    public <T> Iterable<Key<T>> insert(final Iterable<T> entities) {
        return insert(entities, null);
    }

    @Override
    public <T> Iterable<Key<T>> insert(final Iterable<T> entities, final WriteConcern wc) {
        return insert(getCollection(entities.iterator().next()), entities, wc);
    }

    @Override
    public <T> Iterable<Key<T>> insert(final String kind, final Iterable<T> entities) {
        return insert(kind, entities, null);
    }

    @Override
    public <T> Iterable<Key<T>> insert(final String kind, final Iterable<T> entities, final WriteConcern wc) {
        return insert(db.getCollection(kind), entities, wc);
    }

    private <T> Iterable<Key<T>> insert(final DBCollection dbColl, final Iterable<T> entities, final WriteConcern wc) {
        WriteConcern writeConcern = wc;
        final List<Key<T>> savedKeys = new ArrayList<Key<T>>();
        if (morphia.getUseBulkWriteOperations()) {
            BulkWriteOperation bulkWriteOperation = dbColl.initializeOrderedBulkOperation();
            final Map<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
            for (final T entity : entities) {
                if (writeConcern == null) {
                    writeConcern = getWriteConcern(entity);
                }
                DBObject dbObj = toDbObject(entity, involvedObjects);
                bulkWriteOperation.insert(dbObj);
                savedKeys.add(postSaveGetKey(entity, dbObj, dbColl, new LinkedHashMap<Object, DBObject>()));
            }
            bulkWriteOperation.execute(writeConcern);
            postSaveOperations(involvedObjects);

        } else {
            writeConcern = getWriteConcern(entities.iterator().next());
            final List<DBObject> list = new ArrayList<DBObject>();
            for (final T entity : entities) {
                list.add(toDbObject(entity, new LinkedHashMap<Object, DBObject>()));
            }
            dbColl.insert(writeConcern, list.toArray(new DBObject[list.size()]));
            int index = 0;
            for (T entity : entities) {
                savedKeys.add(postSaveGetKey(entity, list.get(index++), dbColl, new LinkedHashMap<Object, DBObject>()));
            }
        }

        return savedKeys;
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

    @Override
    public <T> Iterable<Key<T>> insert(final T... entities) {
        return insert(Arrays.asList(entities), getWriteConcern(entities[0]));
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
    public <T> Key<T> insert(final String kind, final T entity) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        final DBCollection dbColl = getCollection(kind);
        return insert(dbColl, unwrapped, getWriteConcern(unwrapped));
    }

    public <T> Key<T> insert(final String kind, final T entity, final WriteConcern wc) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        final DBCollection dbColl = getCollection(kind);
        return insert(dbColl, unwrapped, wc);
    }

    protected <T> Key<T> insert(final DBCollection dbColl, final T entity, final WriteConcern wc) {
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = entityToDBObj(entity, involvedObjects);
        if (wc == null) {
            dbColl.insert(dbObj);
        } else {
            dbColl.insert(dbObj, wc);
        }

        return postSaveGetKey(entity, dbObj, dbColl, involvedObjects);
    }

    private DBObject entityToDBObj(final Object entity, final Map<Object, DBObject> involvedObjects) {
        return mapper.toDBObject(ProxyHelper.unwrap(entity), involvedObjects);
    }

    /**
     * call postSaveOperations and returns Key for entity
     */
    @SuppressWarnings("unchecked")
    protected <T> Key<T> postSaveGetKey(final T entity, final DBObject dbObj, final DBCollection dbColl,
                                        final Map<Object, DBObject> involvedObjects) {
        if (dbObj.get(Mapper.ID_KEY) == null) {
            throw new MappingException("Missing _id after save!");
        }
        mapper.updateKeyInfo(entity, dbObj, createCache());

        postSaveOperations(involvedObjects);
        final Key<T> key = new Key<T>((Class<? extends T>) entity.getClass(), dbColl.getName(), mapper.getId(entity));
        key.setType((Class<? extends T>) entity.getClass());

        return key;
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

    protected <T> Key<T> save(final DBCollection dbColl, final T entity, final WriteConcern wc) {
        if (entity == null) {
            throw new UpdateException("Can not persist a null entity");
        }

        final MappedClass mc = mapper.getMappedClass(entity);
        if (mc.getAnnotation(NotSaved.class) != null) {
            throw new MappingException(
                                          "Entity type: " + mc.getClazz().getName()
                                          + " is marked as NotSaved which means you should not try to save it!"
            );
        }

        WriteResult wr;

        //involvedObjects is used not only as a cache but also as a list of what needs to be called for life-cycle methods at the end.
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = entityToDBObj(entity, involvedObjects);

        //try to do an update if there is a @Version field
        final Object idValue = dbObj.get(Mapper.ID_KEY);
        wr = tryVersionedUpdate(dbColl, entity, dbObj, idValue, wc, mc);

        if (wr == null) {
            if (wc == null) {
                dbColl.save(dbObj);
            } else {
                dbColl.save(dbObj, wc);
            }
        }

        return postSaveGetKey(entity, dbObj, dbColl, involvedObjects);
    }

    protected <T> WriteResult tryVersionedUpdate(final DBCollection dbColl, final T entity, final DBObject dbObj, final Object idValue,
                                                 final WriteConcern wc, final MappedClass mc) {
        WriteResult wr;
        if (mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            return null;
        }

        final MappedField mfVersion = mc.getFieldsAnnotatedWith(Version.class).get(0);
        final String versionKeyName = mfVersion.getNameToStore();

        Long oldVersion = (Long) mfVersion.getFieldValue(entity);
        long newVersion = nextValue(oldVersion);

        dbObj.put(versionKeyName, newVersion);
        mfVersion.setFieldValue(entity, newVersion);

        if (idValue != null && newVersion != 1) {
            final UpdateResults res = update(find(dbColl.getName(), entity.getClass()).filter(Mapper.ID_KEY, idValue)
                                                                                      .filter(versionKeyName, oldVersion),
                                             dbObj,
                                             false,
                                             false,
                                             wc
                                            );

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

        //update the version.
        return wr;
    }

    @Override
    public <T> Key<T> save(final T entity) {
        return save(entity, getWriteConcern(entity));
    }

    @Override
    public <T> Key<T> save(final String kind, final T entity) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        return save(kind, entity, getWriteConcern(unwrapped));
    }

    @Override
    public <T> Key<T> save(final String kind, final T entity, final WriteConcern wc) {
        return save(getCollection(kind), ProxyHelper.unwrap(entity), wc);
    }

    @Override
    public <T> Key<T> save(final T entity, final WriteConcern wc) {
        final T unwrapped = ProxyHelper.unwrap(entity);
        return save(getCollection(unwrapped), unwrapped, wc);
    }

    @Override
    public <T> UpdateOperations<T> createUpdateOperations(final Class<T> clazz) {
        return new UpdateOpsImpl<T>(clazz, getMapper());
    }

    @Override
    public <T> UpdateOperations<T> createUpdateOperations(final Class<T> kind, final DBObject ops) {
        final UpdateOpsImpl<T> upOps = (UpdateOpsImpl<T>) createUpdateOperations(kind);
        upOps.setOps(ops);
        return upOps;
    }

    @Override
    public <T> UpdateResults update(final Query<T> query, final UpdateOperations<T> ops, final boolean createIfMissing) {
        return update(query, ops, createIfMissing, getWriteConcern(query.getEntityClass()));
    }

    @Override
    public <T> UpdateResults update(final Query<T> query, final UpdateOperations<T> ops, final boolean createIfMissing,
                                    final WriteConcern wc) {
        return update(query, ops, createIfMissing, true, wc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> UpdateResults update(final T ent, final UpdateOperations<T> ops) {
        if (ent instanceof Query) {
            return update((Query<T>) ent, ops);
        }

        final MappedClass mc = mapper.getMappedClass(ent);
        final Query<T> q = (Query<T>) createQuery(mc.getClazz());
        q.disableValidation().filter(Mapper.ID_KEY, mapper.getId(ent));

        if (!mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            final MappedField versionMF = mc.getFieldsAnnotatedWith(Version.class).get(0);
            final Long oldVer = (Long) versionMF.getFieldValue(ent);
            q.filter(versionMF.getNameToStore(), oldVer);
            ops.set(versionMF.getNameToStore(), nextValue(oldVer));
        }

        return update(q, ops);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> UpdateResults update(final Key<T> key, final UpdateOperations<T> ops) {
        Class<T> clazz = (Class<T>) key.getType();
        if (clazz == null) {
            clazz = (Class<T>) mapper.getClassFromCollection(key.getCollection());
        }
        return updateFirst(createQuery(clazz).disableValidation().filter(Mapper.ID_KEY, key.getId()), ops);
    }

    @Override
    public <T> UpdateResults update(final Query<T> query, final UpdateOperations<T> ops) {
        return update(query, ops, false, true);
    }


    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> ops) {
        return update(query, ops, false, false);
    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> ops, final boolean createIfMissing) {
        return update(query, ops, createIfMissing, false);

    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final UpdateOperations<T> ops, final boolean createIfMissing,
                                         final WriteConcern wc) {
        return update(query, ops, createIfMissing, false, wc);
    }

    @Override
    public <T> UpdateResults updateFirst(final Query<T> query, final T entity, final boolean createIfMissing) {
        final LinkedHashMap<Object, DBObject> involvedObjects = new LinkedHashMap<Object, DBObject>();
        final DBObject dbObj = mapper.toDBObject(entity, involvedObjects);

        final UpdateResults res = update(query, dbObj, createIfMissing, false, getWriteConcern(entity));

        //update _id field
        if (res.getInsertedCount() > 0) {
            dbObj.put(Mapper.ID_KEY, res.getNewId());
        }

        postSaveOperations(involvedObjects);
        return res;
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

        //remove (immutable) _id field for update.
        final Object idValue = dbObj.get(Mapper.ID_KEY);
        dbObj.removeField(Mapper.ID_KEY);

        WriteResult wr;

        final MappedClass mc = mapper.getMappedClass(unwrapped);
        final DBCollection dbColl = getCollection(unwrapped);

        //try to do an update if there is a @Version field
        wr = tryVersionedUpdate(dbColl, unwrapped, dbObj, idValue, wc, mc);

        if (wr == null) {
            final Query<T> query = (Query<T>) createQuery(unwrapped.getClass()).filter(Mapper.ID_KEY, id);
            wr = update(query, new BasicDBObject("$set", dbObj), false, false, wc).getWriteResult();
        }

        final UpdateResults res = new UpdateResults(wr);

        if (res.getUpdatedCount() == 0) {
            throw new UpdateException("Nothing updated");
        }

        postSaveOperations(involvedObjects);
        return key;
    }

    private void postSaveOperations(final Map<Object, DBObject> involvedObjects) {
        for (final Map.Entry<Object, DBObject> e : involvedObjects.entrySet()) {
            final Object ent = e.getKey();
            final DBObject dbO = e.getValue();
            final MappedClass mc = mapper.getMappedClass(ent);
            mc.callLifecycleMethods(PostPersist.class, ent, dbO, mapper);
        }
    }

    @SuppressWarnings("rawtypes")
    private <T> UpdateResults update(final Query<T> query, final UpdateOperations ops, final boolean createIfMissing,
                                     final boolean multi,
                                     final WriteConcern wc) {
        final DBObject u = ((UpdateOpsImpl) ops).getOps();
        if (((UpdateOpsImpl) ops).isIsolated()) {
            final Query<T> q = query.cloneQuery();
            q.disableValidation().filter("$atomic", true);
            return update(q, u, createIfMissing, multi, wc);
        }
        return update(query, u, createIfMissing, multi, wc);
    }

    private <T> UpdateResults update(final Query<T> query, final UpdateOperations ops, final boolean createIfMissing,
                                     final boolean multi) {
        return update(query, ops, createIfMissing, multi, getWriteConcern(query.getEntityClass()));
    }

    private <T> UpdateResults update(final Query<T> query, final DBObject u, final boolean createIfMissing, final boolean multi,
                                     final WriteConcern wc) {

        DBCollection dbColl = query.getCollection();
        //TODO remove this after testing.
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

        DBObject q = query.getQueryObject();
        if (q == null) {
            q = new BasicDBObject();
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing update(" + dbColl.getName() + ") for query: " + q + ", ops: " + u + ", multi: " + multi + ", upsert: "
                      + createIfMissing);
        }

        final WriteResult wr;
        if (wc == null) {
            wr = dbColl.update(q, u, createIfMissing, multi);
        } else {
            wr = dbColl.update(q, u, createIfMissing, multi, wc);
        }

        return new UpdateResults(wr);
    }

    @Override
    public <T> T findAndDelete(final Query<T> qi) {
        DBCollection dbColl = qi.getCollection();
        //TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(qi.getEntityClass());
        }

        final EntityCache cache = createCache();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing findAndModify(" + dbColl.getName() + ") with delete ...");
        }

        final DBObject result = dbColl.findAndModify(qi.getQueryObject(),
                                                     qi.getFieldsObject(),
                                                     qi.getSortObject(),
                                                     true,
                                                     null,
                                                     false,
                                                     false);

        if (result != null) {
            return mapper.fromDBObject(qi.getEntityClass(), result, cache);
        }

        return null;
    }

    @Override
    public <T> T findAndModify(final Query<T> q, final UpdateOperations<T> ops) {
        return findAndModify(q, ops, false);
    }

    @Override
    public <T> T findAndModify(final Query<T> query, final UpdateOperations<T> ops, final boolean oldVersion) {
        return findAndModify(query, ops, oldVersion, false);
    }

    @Override
    public <T> T findAndModify(final Query<T> qi, final UpdateOperations<T> ops, final boolean oldVersion, final boolean createIfMissing) {

        DBCollection dbColl = qi.getCollection();
        //TODO remove this after testing.
        if (dbColl == null) {
            dbColl = getCollection(qi.getEntityClass());
        }

        if (LOG.isTraceEnabled()) {
            LOG.info("Executing findAndModify(" + dbColl.getName() + ") with update ");
        }
        DBObject res = null;
        try {
            res = dbColl.findAndModify(qi.getQueryObject(),
                                       qi.getFieldsObject(),
                                       qi.getSortObject(),
                                       false,
                                       ((UpdateOpsImpl<T>) ops).getOps(),
                                       !oldVersion,
                                       createIfMissing);
        } catch (MongoException e) {
            if (e.getMessage() == null || !e.getMessage().contains("matching")) {
                throw e;
            }
        }

        if (res == null) {
            return null;
        } else {
            return mapper.fromDBObject(qi.getEntityClass(), res, createCache());
        }
    }

    @Override
    public <T> MapreduceResults<T> mapReduce(final MapreduceType type, final Query query, final Class<T> outputType,
                                             final MapReduceCommand baseCommand) {

        Assert.parametersNotNull("map", baseCommand.getMap());
        Assert.parameterNotEmpty(baseCommand.getMap(), "map");
        Assert.parametersNotNull("reduce", baseCommand.getReduce());
        Assert.parameterNotEmpty(baseCommand.getMap(), "reduce");

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
            results.setInlineRequiredOptions(outputType, getMapper(), cache);
        } else {
            results.setQuery(newQuery(outputType, db.getCollection(results.getOutputCollectionName())));
        }

        return results;

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

    private EntityCache createCache() {
        return mapper.createEntityCache();
    }

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     */
    public WriteConcern getWriteConcern(final Object clazzOrEntity) {
        WriteConcern wc = defConcern;
        if (clazzOrEntity != null) {
            final Entity entityAnn = getMapper().getMappedClass(clazzOrEntity).getEntityAnnotation();
            if (entityAnn != null && entityAnn.concern() != null && entityAnn.concern().length() != 0) {
                wc = WriteConcern.valueOf(entityAnn.concern());
            }
        }

        return wc;
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
    public DBDecoderFactory setDecoderFact(final DBDecoderFactory fact) {
        decoderFactory = fact;
        return decoderFactory;
    }

    @Override
    public DBDecoderFactory getDecoderFact() {
        return decoderFactory != null ? decoderFactory : DefaultDBDecoder.FACTORY;
    }

    @Override
    public void setQueryFactory(final QueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public QueryFactory getQueryFactory() {
        return queryFactory;
    }

}
