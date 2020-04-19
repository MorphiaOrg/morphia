package dev.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Function;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.DBCollectionFindOptions;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.annotations.Entity;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.query.internal.MappingIterable;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import org.bson.types.CodeWScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.mongodb.CursorType.NonTailable;
import static com.mongodb.CursorType.Tailable;
import static com.mongodb.CursorType.TailableAwait;
import static dev.morphia.query.CriteriaJoin.AND;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


/**
 * Implementation of Query
 *
 * @param <T> The type we will be querying for, and returning.
 */
@SuppressWarnings("deprecation")
public class QueryImpl<T> implements CriteriaContainer, Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(QueryImpl.class);
    private final dev.morphia.DatastoreImpl ds;
    private final DBCollection dbColl;
    private final Class<T> clazz;
    private EntityCache cache;
    private boolean validateName = true;
    private boolean validateType = true;
    private Boolean includeFields;
    private DBObject baseQuery;
    private FindOptions options;
    private CriteriaContainer compoundContainer = new CriteriaContainerImpl(this, AND);

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
    public QueryImpl(final Class<T> clazz, final DBCollection coll, final Datastore ds) {
        this.clazz = clazz;
        this.ds = ((dev.morphia.DatastoreImpl) ds);
        dbColl = coll;
        cache = this.ds.getMapper().createEntityCache();

        final MappedClass mc = this.ds.getMapper().getMappedClass(clazz);
        final Entity entAn = mc == null ? null : mc.getEntityAnnotation();
        if (entAn != null) {
            getOptions().readPreference(this.ds.getMapper().getMappedClass(clazz).getEntityAnnotation().queryNonPrimary()
                                        ? ReadPreference.secondaryPreferred()
                                        : null);
        }
    }

    /**
     * Parses the string and validates each part
     *
     * @param str      the String to parse
     * @param clazz    the class to use when validating
     * @param mapper   the Mapper to use
     * @param validate true if the results should be validated
     * @return the DBObject
     * @deprecated this is an internal method and will be removed in the next version
     */
    @Deprecated
    public static BasicDBObject parseFieldsString(final String str, final Class clazz, final Mapper mapper, final boolean validate) {
        BasicDBObject ret = new BasicDBObject();
        final String[] parts = str.split(",");
        for (String s : parts) {
            s = s.trim();
            int dir = 1;

            if (s.startsWith("-")) {
                dir = -1;
                s = s.substring(1).trim();
            }

            if (validate) {
                s = new PathTarget(mapper, clazz, s).translatedPath();
            }
            ret.put(s, dir);
        }
        return ret;
    }

    @Override
    public MorphiaKeyCursor<T> keys() {
        return keys(new FindOptions());
    }

    @Override
    public MorphiaKeyCursor<T> keys(final FindOptions options) {
        QueryImpl<T> cloned = cloneQuery();
        cloned.getOptions().projection(new BasicDBObject("_id", 1));
        cloned.includeFields = true;

        return new MorphiaKeyCursor<T>(ds, cloned.prepareCursor(options), ds.getMapper(), clazz, dbColl.getName());
    }

    @Override
    public List<Key<T>> asKeyList() {
        return asKeyList(getOptions());
    }

    @Override
    public List<Key<T>> asKeyList(final FindOptions options) {
        return keys(options).toList();
    }

    @Override
    public List<T> asList() {
        return find(getOptions()).toList();
    }

    @Override
    public List<T> asList(final FindOptions options) {
        return find(options).toList();
    }

    @Override
    @Deprecated
    public long countAll() {
        final DBObject query = getQueryObject();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing count(" + dbColl.getName() + ") for query: " + query);
        }
        return dbColl.getCount(query);
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
    public MorphiaIterator<T, T> fetch() {
        return fetch(getOptions());
    }

    @Override
    public MorphiaIterator<T, T> fetch(final FindOptions options) {
        final DBCursor cursor = prepareCursor(options);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting cursor(" + dbColl.getName() + ")  for query:" + cursor.getQuery());
        }

        return new MorphiaIterator<T, T>(ds, cursor, ds.getMapper(), clazz, dbColl.getName(), cache);
    }

    @Override
    public MorphiaCursor<T> find() {
        return find(getOptions());
    }

    @Override
    public MorphiaCursor<T> find(final FindOptions options) {
        return new MorphiaCursor<T>(ds, prepareCursor(options), ds.getMapper(), clazz, cache);
    }

    @Override
    public MorphiaIterator<T, T> fetchEmptyEntities() {
        return fetchEmptyEntities(getOptions());
    }

    @Override
    public MorphiaIterator<T, T> fetchEmptyEntities(final FindOptions options) {
        QueryImpl<T> cloned = cloneQuery();
        cloned.getOptions().projection(new BasicDBObject("_id", 1));
        cloned.includeFields = true;
        return cloned.fetch(options);
    }

    @Override
    public MorphiaKeyIterator<T> fetchKeys() {
        return fetchKeys(getOptions());
    }

    @Override
    public MorphiaKeyIterator<T> fetchKeys(final FindOptions options) {
        QueryImpl<T> cloned = cloneQuery();
        final FindOptions opts = cloned.getOptions();
        opts.projection(new BasicDBObject("_id", 1));
        cloned.includeFields = true;

        return new MorphiaKeyIterator<T>(ds, cloned.prepareCursor(options), ds.getMapper(), clazz, dbColl.getName());
    }

    @Override
    public T first() {
        final MongoCursor<T> iterator = iterator();
        try {
            return iterator.tryNext();
        } finally {
            iterator.close();
        }
    }

    @Override
    public T first(final FindOptions options) {
        final MongoCursor<T> it = find(options.copy().limit(1));
        try {
            return it.tryNext();
        } finally {
            it.close();
        }
    }

    @Override
    public T get() {
        return first(getOptions());
    }

    @Override
    public T get(final FindOptions options) {
        return first(options);
    }

    @Override
    public Key<T> getKey() {
        return getKey(getOptions());
    }

    @Override
    public Key<T> getKey(final FindOptions options) {
        final MongoCursor<Key<T>> it = keys(options.copy().limit(1));
        try {
            return it.tryNext();
        } finally {
            it.close();
        }
    }

    @Override
    @Deprecated
    public MorphiaIterator<T, T> tail() {
        return tail(true);
    }

    @Override
    @Deprecated
    public MorphiaIterator<T, T> tail(final boolean awaitData) {
        return fetch(getOptions()
                         .copy()
                         .cursorType(awaitData ? TailableAwait : Tailable));
    }

    @Override
    @Deprecated
    public Query<T> batchSize(final int value) {
        getOptions().batchSize(value);
        return this;
    }

    @Override
    public QueryImpl<T> cloneQuery() {
        final QueryImpl<T> n = new QueryImpl<T>(clazz, dbColl, ds);
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
    @Deprecated
    public Query<T> comment(final String comment) {
        getOptions().comment(comment);
        return this;
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(this, AND);
        add(container);

        return new FieldEndImpl<CriteriaContainer>(this, field, container);
    }

    @Override
    @Deprecated
    public Query<T> disableCursorTimeout() {
        getOptions().noCursorTimeout(true);
        return this;
    }

    @Override
    public Query<T> disableValidation() {
        validateName = false;
        validateType = false;
        return this;
    }

    @Override
    @Deprecated
    public Query<T> enableCursorTimeout() {
        getOptions().noCursorTimeout(false);
        return this;
    }

    @Override
    public Query<T> enableValidation() {
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
    public FieldEnd<? extends Query<T>> field(final String name) {
        return new FieldEndImpl<QueryImpl<T>>(this, name, this);
    }

    @Override
    public Query<T> filter(final String condition, final Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final String prop = parts[0].trim();
        final FilterOperator op = (parts.length == 2) ? translate(parts[1]) : FilterOperator.EQUAL;

        add(new FieldCriteria(this, prop, op, value));

        return this;
    }

    @Override
    @Deprecated
    public int getBatchSize() {
        return getOptions().getBatchSize();
    }

    @Override
    @Deprecated
    public DBCollection getCollection() {
        return dbColl;
    }

    @Override
    public Class<T> getEntityClass() {
        return clazz;
    }

    @Override
    @Deprecated
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

    @Override
    @Deprecated
    public int getLimit() {
        return getOptions().getLimit();
    }

    @Override
    @Deprecated
    public int getOffset() {
        return getOptions().getSkip();
    }

    @Override
    @Deprecated
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

    @Override
    @Deprecated
    public DBObject getSortObject() {
        DBObject sort = getOptions().getSortDBObject();
        return (sort == null) ? null : new BasicDBObject(sort.toMap());
    }

    @Override
    @Deprecated
    public Query<T> hintIndex(final String idxName) {
        getOptions().hint(new BasicDBObject(idxName, 1));
        return this;
    }

    @Override
    @Deprecated
    public Query<T> limit(final int value) {
        getOptions().limit(value);
        return this;
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public Query<T> lowerIndexBound(final DBObject lowerBound) {
        if (lowerBound != null) {
            getOptions().min(lowerBound);
        }
        return this;
    }

    @Override
    public void forEach(final Consumer<? super T> block) {
        MongoCursor<T> cursor = iterator();
        try {
            while (cursor.hasNext()) {
                block.accept(cursor.next());
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    @Deprecated
    public Query<T> maxTime(final long value, final TimeUnit unit) {
        getOptions().maxTime(value, unit);
        return this;
    }

    @Override
    @Deprecated
    public Query<T> offset(final int value) {
        getOptions().skip(value);
        return this;
    }

    @Override
    public Query<T> order(final String sort) {
        getOptions().sort(parseFieldsString(sort, clazz, ds.getMapper(), validateName));
        return this;
    }

    @Override
    public Query<T> order(final Meta sort) {
        getOptions().sort(sort.toDatabase());

        return this;
    }

    @Override
    public Query<T> order(final Sort... sorts) {
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
    public Query<T> queryNonPrimary() {
        getOptions().readPreference(ReadPreference.secondaryPreferred());
        return this;
    }

    @Override
    @Deprecated
    public Query<T> queryPrimaryOnly() {
        getOptions().readPreference(ReadPreference.primary());
        return this;
    }

    @Override
    public Query<T> retrieveKnownFields() {
        final MappedClass mc = ds.getMapper().getMappedClass(clazz);
        final List<String> fields = new ArrayList<String>(mc.getPersistenceFields().size() + 1);
        for (final MappedField mf : mc.getPersistenceFields()) {
            fields.add(mf.getNameToStore());
        }
        retrievedFields(true, fields.toArray(new String[0]));
        return this;
    }

    @Override
    public Query<T> project(final String field, final boolean include) {
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
    public Query<T> project(final String field, final ArraySlice slice) {
        final Mapper mapper = ds.getMapper();
        String fieldName = new PathTarget(mapper, mapper.getMappedClass(clazz), field, validateName).translatedPath();
        validateProjections(fieldName, true);
        project(fieldName, slice.toDatabase());
        return this;
    }

    @Override
    public Query<T> project(final Meta meta) {
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
    public Query<T> retrievedFields(final boolean include, final String... list) {
        if (includeFields != null && include != includeFields) {
            throw new IllegalStateException("You cannot mix included and excluded fields together");
        }
        for (String field : list) {
            project(field, include);
        }
        return this;
    }

    @Override
    public <A extends Collection<? super T>> A into(final A target) {
        forEach((t) -> target.add(t));
        return target;
    }

    @Override
    public Query<T> search(final String search) {
        final BasicDBObject op = new BasicDBObject("$search", search);

        this.criteria("$text").equal(op);

        return this;
    }

    @Override
    public Query<T> search(final String search, final String language) {

        final BasicDBObject op = new BasicDBObject("$search", search)
                                     .append("$language", language);

        this.criteria("$text").equal(op);

        return this;
    }

    @Override
    public Query<T> maxScan(final int value) {
        throw new UnsupportedOperationException("this method is deprecated");
    }

    @Override
    @Deprecated
    public Query<T> useReadPreference(final ReadPreference readPref) {
        getOptions().readPreference(readPref);
        return this;
    }

    @Override
    public Query<T> where(final String js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public Query<T> where(final CodeWScope js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException("this method is unused on a Query");
    }

    /**
     * @return the Datastore
     * @deprecated this is an internal method that exposes an internal type and will likely go away soon
     */
    @Deprecated
    public dev.morphia.DatastoreImpl getDatastore() {
        return ds;
    }

    /**
     * @return true if field names are being validated
     */
    public boolean isValidatingNames() {
        return validateName;
    }

    /**
     * @return true if query parameter value types are being validated against the field types
     */
    public boolean isValidatingTypes() {
        return validateType;
    }

    @Override
    public MongoCursor<T> iterator() {
        return find();
    }

    @Override
    public MongoCursor<T> cursor() {
        return find();
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
            LOG.trace(String.format("Running query(%s) : %s, options: %s,", dbColl.getName(), query, findOptions));
        }

        if (findOptions.getCursorType() != NonTailable && (findOptions.getSortDBObject() != null)) {
            LOG.warn("Sorting on tail is not allowed.");
        }

        return dbColl.find(query, findOptions.getOptions()
                                             .copy()
                                             .sort(getSortObject())
                                             .projection(getFieldsObject()))
                     .setDecoderFactory(ds.getDecoderFact());
    }

    @Override
    public <U> MongoIterable<U> map(final Function<T, U> mapper) {
        return new MappingIterable<T, U>(this, mapper);
    }

    @Override
    @Deprecated
    public Query<T> returnKey() {
        getOptions().returnKey(true);
        return this;
    }

    @Override
    @Deprecated
    public Query<T> upperIndexBound(final DBObject upperBound) {
        if (upperBound != null) {
            getOptions().max(upperBound);
        }

        return this;
    }

    @Override
    public String toString() {
        return String.format("{ %s %s }", getQueryObject(), getOptions().getProjection() == null
                                                            ? ""
                                                            : ", projection: " + getFieldsObject());
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
}
