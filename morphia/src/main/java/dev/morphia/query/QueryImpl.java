package dev.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.DBCollectionFindOptions;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.Key;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Version;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import org.bson.Document;
import org.bson.types.CodeWScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mongodb.CursorType.NonTailable;
import static dev.morphia.query.CriteriaJoin.AND;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


/**
 * Implementation of Query
 *
 * @param <R> The type we will be querying for, and returning.
 */
public class QueryImpl<R> implements CriteriaContainer, Query<R> {
    private static final Logger LOG = LoggerFactory.getLogger(QueryImpl.class);
    final Datastore ds;
    final DBCollection dbColl;
    final Class<R> clazz;
    final Mapper mapper;
    private EntityCache cache;
    private boolean validateName = true;
    private boolean validateType = true;
    private Boolean includeFields;
    private DBObject baseQuery;
    private FindOptions options;
    private CriteriaContainer compoundContainer;

    FindOptions getOptions() {
        if (options == null) {
            options = new FindOptions();
        }
        return options;
    }

    /**
     * Creates a Query for the given type and collection
     *
     * @param clazz the type to return
     * @param coll  the collection to query
     * @param ds    the Datastore to use
     */
    public QueryImpl(final Class<R> clazz, final DBCollection coll, final Datastore ds) {
        this.clazz = clazz;
        this.ds = ds;
        dbColl = coll;
        mapper = this.ds.getMapper();
        cache = mapper.createEntityCache();

        final MappedClass mc = mapper.getMappedClass(clazz);
        final Entity entAn = mc == null ? null : mc.getEntityAnnotation();
        if (entAn != null) {
            getOptions().readPreference(mapper.getMappedClass(clazz).getEntityAnnotation().queryNonPrimary()
                                        ? ReadPreference.secondaryPreferred()
                                        : null);
        }
        compoundContainer = new CriteriaContainerImpl(mapper, this, AND);
    }

    @Override
    public MorphiaKeyCursor<R> keys() {
        return keys(new FindOptions());
    }

    @Override
    public MorphiaKeyCursor<R> keys(final FindOptions options) {
        QueryImpl<R> cloned = cloneQuery();
        cloned.getOptions().projection(new BasicDBObject("_id", 1));
        cloned.includeFields = true;

        return new MorphiaKeyCursor<>(ds, cloned.prepareCursor(options), ds.getMapper(), clazz, dbColl.getName());
    }

    @Override
    public long count() {
        return dbColl.getCount(getQueryObject());
    }

    @Override
    public long count(final CountOptions options) {
        return dbColl.getCount(getQueryObject(), options.getOptions());
    }

    @Override
    public MorphiaCursor<R> execute() {
        return this.execute(getOptions());
    }

    @Override
    public MorphiaCursor<R> execute(final FindOptions options) {
        return new MorphiaCursor<>(ds, prepareCursor(options), ds.getMapper(), clazz, cache);
    }

    @Override
    public R first() {
        try (MongoCursor<R> iterator = this.execute()) {
            return iterator.tryNext();
        }
    }

    @Override
    public R first(final FindOptions options) {
        try (MongoCursor<R> it = this.execute(options.copy().limit(1))) {
            return it.tryNext();
        }
    }

    @Override
    public Key<R> getKey() {
        return getKey(getOptions());
    }

    @Override
    public Key<R> getKey(final FindOptions options) {
        try (MongoCursor<Key<R>> it = keys(options.copy().limit(1))) {
            return it.tryNext();
        }
    }

    @Override
    @Deprecated
    public Query<R> batchSize(final int value) {
        getOptions().batchSize(value);
        return this;
    }

    /**
     * @morphia.internal
     */
    QueryImpl<R> cloneQuery() {
        final QueryImpl<R> n = new QueryImpl<>(clazz, dbColl, ds);
        n.cache = ds.getMapper().createEntityCache(); // fresh cache
        n.includeFields = includeFields;
        n.validateName = validateName;
        n.validateType = validateType;
        n.baseQuery = copy(baseQuery);
        n.options = options != null ? options.copy() : null;
        n.compoundContainer = compoundContainer;
        return n;
    }

    private BasicDBObject copy(final DBObject dbObject) {
        return dbObject == null ? null : new BasicDBObject(dbObject.toMap());
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(mapper, this, AND);
        add(container);

        return new FieldEndImpl<CriteriaContainer>(mapper, this, field, container);
    }

    @Override
    public Query<R> disableValidation() {
        validateName = false;
        validateType = false;
        return this;
    }

    @Override
    public Query<R> enableValidation() {
        validateName = true;
        validateType = true;
        return this;
    }

    @Override
    public Map<String, Object> explain() {
        return explain(getOptions());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> explain(final FindOptions options) {
        return prepareCursor(options).explain().toMap();
    }

    @Override
    public FieldEnd<? extends Query<R>> field(final String name) {
        return new FieldEndImpl<>(mapper, this, name, this);
    }

    @Override
    public Query<R> filter(final String condition, final Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final String prop = parts[0].trim();
        final FilterOperator op = (parts.length == 2) ? translate(parts[1]) : FilterOperator.EQUAL;

        add(new FieldCriteria(mapper, this, prop, op, value));

        return this;
    }

    @Override
    @Deprecated
    public int getBatchSize() {
        return getOptions().getBatchSize();
    }

    /**
     * @return the collection this query targets
     * @morphia.internal
     */
    public DBCollection getCollection() {
        return dbColl;
    }

    /**
     * @return the entity {@link Class}.
     * @morphia.internal
     */
    public Class<R> getEntityClass() {
        return clazz;
    }

    /**
     * @return the Mongo fields {@link DBObject}.
     * @morphia.internal
     */
    public DBObject getFieldsObject() {
        DBObject projection = getOptions().getProjection();
        if (projection == null || projection.keySet().isEmpty()) {
            return null;
        }

        final MappedClass mc = ds.getMapper().getMappedClass(clazz);

        Entity entityAnnotation = mc.getEntityAnnotation();
        final BasicDBObject fieldsFilter = copy(projection);

        if (includeFields && entityAnnotation != null && !entityAnnotation.noClassnameStored()) {
            fieldsFilter.put(ds.getMapper().getOptions().getDiscriminatorField(), 1);
        }

        return fieldsFilter;
    }

    @Deprecated
    private int getLimit() {
        return getOptions().getLimit();
    }

    @Override
    @Deprecated
    public int getOffset() {
        return getOptions().getSkip();
    }

    /**
     * @return the query object
     * @morphia.internal
     */
    public DBObject getQueryObject() {
        final DBObject obj = new BasicDBObject();

        if (baseQuery != null) {
            obj.putAll(baseQuery.toMap());
        }

        obj.putAll(toDBObject());

        return obj;
    }

    /**
     * Sets query structure directly
     *
     * @param query the DBObject containing the query
     */
    public void setQueryObject(final DBObject query) {
        baseQuery = new BasicDBObject(query.toMap());
    }

    /**
     * @return the Mongo sort {@link DBObject}.
     * @morphia.internal
     */
    public DBObject getSortObject() {
        DBObject sort = getOptions().getSortDBObject();
        return (sort == null) ? null : new BasicDBObject(sort.toMap());
    }

    @Override
    @Deprecated
    public Query<R> hintIndex(final String idxName) {
        getOptions().modifier("$hint", idxName);
        return this;
    }

    @Override
    @Deprecated
    public Query<R> limit(final int value) {
        getOptions().limit(value);
        return this;
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public Query<R> lowerIndexBound(final DBObject lowerBound) {
        if (lowerBound != null) {
            getOptions().modifier("$min", new Document(lowerBound.toMap()));
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<R> maxScan(final int value) {
        if (value > 0) {
            getOptions().modifier("$maxScan", value);
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<R> maxTime(final long value, final TimeUnit unit) {
        getOptions().maxTime(value, unit);
        return this;
    }

    @Override
    @Deprecated
    public Query<R> offset(final int value) {
        getOptions().skip(value);
        return this;
    }

    @Override
    public Query<R> order(final Meta sort) {
        getOptions().sort(sort.toDatabase());

        return this;
    }

    @Override
    public Query<R> order(final Sort... sorts) {
        BasicDBObject sortList = new BasicDBObject();
        for (Sort sort : sorts) {
            String s = sort.getField();
            if (validateName) {
                s = new PathTarget(ds.getMapper(), clazz, s).translatedPath();
            }
            sortList.put(s, sort.getOrder());
        }
        getOptions().sort(sortList);
        return this;
    }

    @Override
    @Deprecated
    public Query<R> queryNonPrimary() {
        getOptions().readPreference(ReadPreference.secondaryPreferred());
        return this;
    }

    @Override
    @Deprecated
    public Query<R> queryPrimaryOnly() {
        getOptions().readPreference(ReadPreference.primary());
        return this;
    }

    @Override
    public Query<R> retrieveKnownFields() {
        final MappedClass mc = ds.getMapper().getMappedClass(clazz);
        final List<String> fields = new ArrayList<>(mc.getPersistenceFields().size() + 1);
        for (final MappedField mf : mc.getPersistenceFields()) {
            fields.add(mf.getNameToStore());
        }
        retrievedFields(true, fields.toArray(new String[0]));
        return this;
    }

    @Override
    public Query<R> project(final String field, final boolean include) {
        final Mapper mapper = ds.getMapper();
        String fieldName = new PathTarget(mapper, mapper.getMappedClass(clazz), field, validateName).translatedPath();
        validateProjections(fieldName, include);
        project(fieldName, include ? 1 : 0);
        return this;
    }

    private void project(final String fieldName, final Object value) {
        DBObject projection = getOptions().getProjection();
        if (projection == null) {
            projection = new BasicDBObject();
            getOptions().projection(projection);
        }
        projection.put(fieldName, value);
    }

    private void project(final DBObject value) {
        DBObject projection = getOptions().getProjection();
        if (projection == null) {
            projection = new BasicDBObject();
            getOptions().projection(projection);
        }
        projection.putAll(value);
    }

    @Override
    public Query<R> project(final String field, final ArraySlice slice) {
        final Mapper mapper = ds.getMapper();
        String fieldName = new PathTarget(mapper, mapper.getMappedClass(clazz), field, validateName).translatedPath();
        validateProjections(fieldName, true);
        project(fieldName, slice.toDatabase());
        return this;
    }

    @Override
    public Query<R> project(final Meta meta) {
        final Mapper mapper = ds.getMapper();
        String fieldName = new PathTarget(mapper, clazz, meta.getField(), false).translatedPath();
        validateProjections(fieldName, true);
        project(meta.toDatabase());
        return this;

    }

    private void validateProjections(final String field, final boolean include) {
        if (includeFields != null && include != includeFields) {
            if (!includeFields || !"_id".equals(field)) {
                throw new ValidationException("You cannot mix included and excluded fields together");
            }
        }
        if (includeFields == null) {
            includeFields = include;
        }
    }

    @Override
    @Deprecated
    public Query<R> retrievedFields(final boolean include, final String... list) {
        if (includeFields != null && include != includeFields) {
            throw new IllegalStateException("You cannot mix included and excluded fields together");
        }
        for (String field : list) {
            project(field, include);
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<R> returnKey() {
        getOptions().getModifiers().put("$returnKey", true);
        return this;
    }

    @Override
    public Query<R> search(final String search) {

        final BasicDBObject op = new BasicDBObject("$search", search);

        this.criteria("$text").equal(op);

        return this;
    }

    @Override
    public Query<R> search(final String search, final String language) {

        final BasicDBObject op = new BasicDBObject("$search", search)
                                     .append("$language", language);

        this.criteria("$text").equal(op);

        return this;
    }

    @Override
    @Deprecated
    public Query<R> upperIndexBound(final DBObject upperBound) {
        if (upperBound != null) {
            getOptions().getModifiers().put("$max", new BasicDBObject(upperBound.toMap()));
        }

        return this;
    }

    @Override
    @Deprecated
    public Query<R> useReadPreference(final ReadPreference readPref) {
        getOptions().readPreference(readPref);
        return this;
    }

    @Override
    public Query<R> where(final String js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public Query<R> where(final CodeWScope js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public Modify modify() {
        return new Modify(this);
    }

    @Override
    public Modify modify(final UpdateOperations<R> operations) {
        Modify modify = modify();
        modify.setOps(((UpdateOpsImpl) operations).getOps());

        return modify;
    }

    @Override
    public WriteResult remove(final DeleteOptions options) {
        return getCollection()
                   .remove(getQueryObject(), enforceWriteConcern(options, getEntityClass()).getOptions());
    }

    @Override
    public Update update() {
        return new Update(ds, mapper, clazz, dbColl, getQueryObject());
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public Update update(UpdateOperations operations) {
        final Update updates = update();
        updates.setOps(((UpdateOpsImpl) operations).getOps());

        return updates;
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public Update update(DBObject dbObject) {
        final Update updates = update();
        updates.setOps(dbObject);

        return updates;
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException("this method is unused on a Query");
    }

    /**
     * @return true if field names are being validated
     */
    public boolean isValidatingNames() {
        return validateName;
    }

    /**
     * Prepares cursor for iteration
     *
     * @return the cursor
     * @deprecated this is an internal method.  no replacement is planned.
     */
    @Deprecated
    public DBCursor prepareCursor() {
        return prepareCursor(getOptions());
    }

    private DBCursor prepareCursor(final FindOptions findOptions) {
        final DBObject query = getQueryObject();

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Running query(%s) : %s, options: %s,", dbColl.getName(), query, findOptions));
        }

        if (findOptions.getCursorType() != NonTailable && (findOptions.getSortDBObject() != null)) {
            LOG.warn("Sorting on tail is not allowed.");
        }

        return dbColl.find(query, findOptions.getOptions()
                                             .copy()
                                             .sort(getSortObject())
                                             .projection(getFieldsObject()));
    }

    @Override
    public String toString() {
        return getOptions().getProjection() == null ? getQueryObject().toString()
                                                    : format("{ %s,  %s }", getQueryObject(), getFieldsObject());
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    private FilterOperator translate(final String operator) {
        return FilterOperator.fromString(operator);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryImpl)) {
            return false;
        }

        final QueryImpl<?> query = (QueryImpl<?>) o;

        if (validateName != query.validateName) {
            return false;
        }
        if (validateType != query.validateType) {
            return false;
        }
        if (!dbColl.equals(query.dbColl)) {
            return false;
        }
        if (!clazz.equals(query.clazz)) {
            return false;
        }
        if (includeFields != null ? !includeFields.equals(query.includeFields) : query.includeFields != null) {
            return false;
        }
        if (baseQuery != null ? !baseQuery.equals(query.baseQuery) : query.baseQuery != null) {
            return false;
        }
        return compare(options, query.options);

    }

    private boolean compare(final FindOptions these, final FindOptions those) {
        if (these == null && those != null || these != null && those == null) {
            return false;
        }
        if (these == null) {
            return true;
        }

        DBCollectionFindOptions dbOptions = these.getOptions();
        DBCollectionFindOptions that = those.getOptions();

        if (dbOptions.getBatchSize() != that.getBatchSize()) {
            return false;
        }
        if (dbOptions.getLimit() != that.getLimit()) {
            return false;
        }
        if (dbOptions.getMaxTime(MILLISECONDS) != that.getMaxTime(MILLISECONDS)) {
            return false;
        }
        if (dbOptions.getMaxAwaitTime(MILLISECONDS) != that.getMaxAwaitTime(MILLISECONDS)) {
            return false;
        }
        if (dbOptions.getSkip() != that.getSkip()) {
            return false;
        }
        if (dbOptions.isNoCursorTimeout() != that.isNoCursorTimeout()) {
            return false;
        }
        if (dbOptions.isOplogReplay() != that.isOplogReplay()) {
            return false;
        }
        if (dbOptions.isPartial() != that.isPartial()) {
            return false;
        }
        if (!dbOptions.getModifiers().equals(that.getModifiers())) {
            return false;
        }
        if (dbOptions.getProjection() != null ? !dbOptions.getProjection().equals(that.getProjection()) : that.getProjection() != null) {
            return false;
        }
        if (dbOptions.getSort() != null ? !dbOptions.getSort().equals(that.getSort()) : that.getSort() != null) {
            return false;
        }
        if (dbOptions.getCursorType() != that.getCursorType()) {
            return false;
        }
        if (dbOptions.getReadPreference() != null ? !dbOptions.getReadPreference().equals(that.getReadPreference())
                                                  : that.getReadPreference() != null) {
            return false;
        }
        if (dbOptions.getReadConcern() != null ? !dbOptions.getReadConcern().equals(that.getReadConcern())
                                               : that.getReadConcern() != null) {
            return false;
        }
        return dbOptions.getCollation() != null ? dbOptions.getCollation().equals(that.getCollation()) : that.getCollation() == null;

    }

    private int hash(final FindOptions options) {
        if (options == null) {
            return 0;
        }
        int result = options.getBatchSize();
        result = 31 * result + getLimit();
        result = 31 * result + options.getModifiers().hashCode();
        result = 31 * result + (options.getProjection() != null ? options.getProjection().hashCode() : 0);
        result = 31 * result + (int) (options.getMaxTime(MILLISECONDS) ^ options.getMaxTime(MILLISECONDS) >>> 32);
        result = 31 * result + (int) (options.getMaxAwaitTime(MILLISECONDS) ^ options.getMaxAwaitTime(MILLISECONDS) >>> 32);
        result = 31 * result + options.getSkip();
        result = 31 * result + (options.getSortDBObject() != null ? options.getSortDBObject().hashCode() : 0);
        result = 31 * result + options.getCursorType().hashCode();
        result = 31 * result + (options.isNoCursorTimeout() ? 1 : 0);
        result = 31 * result + (options.isOplogReplay() ? 1 : 0);
        result = 31 * result + (options.isPartial() ? 1 : 0);
        result = 31 * result + (options.getReadPreference() != null ? options.getReadPreference().hashCode() : 0);
        result = 31 * result + (options.getReadConcern() != null ? options.getReadConcern().hashCode() : 0);
        result = 31 * result + (options.getCollation() != null ? options.getCollation().hashCode() : 0);
        return result;
    }

    @Override
    public int hashCode() {
        int result = dbColl.hashCode();
        result = 31 * result + clazz.hashCode();
        result = 31 * result + (validateName ? 1 : 0);
        result = 31 * result + (validateType ? 1 : 0);
        result = 31 * result + (includeFields != null ? includeFields.hashCode() : 0);
        result = 31 * result + (baseQuery != null ? baseQuery.hashCode() : 0);
        result = 31 * result + hash(options);
        return result;
    }

    @Override
    public void add(final Criteria... criteria) {
        for (final Criteria c : criteria) {
            c.attach(this);
            compoundContainer.add(c);
        }
    }

    @Override
    public CriteriaContainer and(final Criteria... criteria) {
        return compoundContainer.and(criteria);
    }

    @Override
    public CriteriaContainer or(final Criteria... criteria) {
        return compoundContainer.or(criteria);
    }

    @Override
    public DBObject toDBObject() {
        return compoundContainer.toDBObject();
    }

    @Override
    public void remove(final Criteria criteria) {
        compoundContainer.remove(criteria);
    }

    @Override
    public void attach(final CriteriaContainer container) {
        compoundContainer.attach(container);
    }

    private DeleteOptions enforceWriteConcern(final DeleteOptions options, final Class<R> klass) {
        if (options.getWriteConcern() == null) {
            return options
                       .copy()
                       .writeConcern(getWriteConcern(klass));
        }
        return options;
    }

    WriteConcern getWriteConcern(final Object clazzOrEntity) {
        WriteConcern wc = ds.getMongo().getWriteConcern();
        if (clazzOrEntity != null) {
            final Entity entityAnn = mapper.getMappedClass(clazzOrEntity).getEntityAnnotation();
            if (entityAnn != null && !entityAnn.concern().isEmpty()) {
                wc = WriteConcern.valueOf(entityAnn.concern());
            }
        }

        return wc;
    }

    public static class Update extends UpdatesImpl<Update> {
        private DBObject queryObject;
        private DBCollection dbColl;

        Update(final Datastore datastore, final Mapper mapper, final Class clazz, final DBCollection collection,
               final DBObject queryObject) {
            super(datastore, mapper, clazz);
            dbColl = collection;
            this.queryObject = queryObject;
        }

        public UpdateResults execute() {
            return execute(new UpdateOptions());
        }

        public UpdateResults execute(final UpdateOptions options) {

            final List<MappedField> fields = mapper.getMappedClass(clazz)
                                                   .getFieldsAnnotatedWith(Version.class);
            if (!fields.isEmpty()) {
                inc(fields.get(0).getNameToStore(), 1);
            }

            return new UpdateResults(dbColl.update(queryObject, getOps(),
                enforceWriteConcern(options, clazz)
                    .getOptions()));
        }

    }

}

