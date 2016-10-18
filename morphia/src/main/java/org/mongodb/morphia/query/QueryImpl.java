package org.mongodb.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.DBCollectionFindOptions;
import org.bson.BSONObject;
import org.bson.Document;
import org.bson.types.CodeWScope;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mongodb.CursorType.NonTailable;
import static com.mongodb.CursorType.Tailable;
import static com.mongodb.CursorType.TailableAwait;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mongodb.morphia.query.QueryValidator.validateQuery;


/**
 * Implementation of Query
 *
 * @param <T> The type we will be querying for, and returning.
 * @author Scott Hernandez
 */
@SuppressWarnings("deprecation")
public class QueryImpl<T> extends CriteriaContainerImpl implements Query<T> {
    private static final Logger LOG = MorphiaLoggerFactory.get(QueryImpl.class);
    private final DatastoreImpl ds;
    private final DBCollection dbColl;
    private final Class<T> clazz;
    private EntityCache cache;
    private boolean validateName = true;
    private boolean validateType = true;
    private Boolean includeFields;
    private BasicDBObject baseQuery;
    private FindOptions options;

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
        super(CriteriaJoin.AND);

        setQuery(this);
        this.clazz = clazz;
        this.ds = ((DatastoreImpl) ds);
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
     */
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
                final StringBuilder sb = new StringBuilder(s);
                validateQuery(clazz, mapper, sb, FilterOperator.IN, "", true, false);
                s = sb.toString();
            }
            ret.put(s, dir);
        }
        return ret;
    }

    @Override
    public List<Key<T>> asKeyList() {
        return asKeyList(getOptions());
    }

    @Override
    public List<Key<T>> asKeyList(final FindOptions options) {
        final List<Key<T>> results = new ArrayList<Key<T>>();
        MorphiaKeyIterator<T> keys = fetchKeys(options);
        try {
            for (final Key<T> key : keys) {
                results.add(key);
            }
        } finally {
            keys.close();
        }
        return results;
    }

    @Override
    public List<T> asList() {
        return asList(getOptions());
    }

    @Override
    public List<T> asList(final FindOptions options) {
        final List<T> results = new ArrayList<T>();
        final MorphiaIterator<T, T> iter = fetch(options);
        try {
            for (final T ent : iter) {
                results.add(ent);
            }
        } finally {
            iter.close();
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("asList: %s \t %d entities, iterator time: driver %d ms, mapper %d ms %n\t cache: %s %n\t for %s",
                             dbColl.getName(), results.size(), iter.getDriverTime(), iter.getMapperTime(), cache.stats(),
                             getQueryObject()));
        }

        return results;
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
    public MorphiaIterator<T, T> fetchEmptyEntities() {
        return fetchEmptyEntities(getOptions());
    }

    @Override
    public MorphiaIterator<T, T> fetchEmptyEntities(final FindOptions options) {
        QueryImpl<T> cloned = cloneQuery();
        cloned.getOptions().projection(new BasicDBObject(Mapper.ID_KEY, 1));
        cloned.includeFields = true;
        return cloned.fetch();
    }

    @Override
    public MorphiaKeyIterator<T> fetchKeys() {
        return fetchKeys(getOptions());
    }

    @Override
    public MorphiaKeyIterator<T> fetchKeys(final FindOptions options) {
        QueryImpl<T> cloned = cloneQuery();
        cloned.getOptions().projection(new BasicDBObject(Mapper.ID_KEY, 1));
        cloned.includeFields = true;

        return new MorphiaKeyIterator<T>(ds, cloned.prepareCursor(options), ds.getMapper(), clazz, dbColl.getName());
    }

    @Override
    public T get() {
        return get(getOptions());
    }

    @Override
    public T get(final FindOptions options) {
        final MorphiaIterator<T, T> it = fetch(options
                                                   .copy()
                                                   .limit(1));
        try {
            return (it.hasNext()) ? it.next() : null;
        } finally {
            it.close();
        }
    }

    @Override
    public Key<T> getKey() {
        return getKey(getOptions());
    }

    @Override
    public Key<T> getKey(final FindOptions options) {
        final MorphiaIterator<T, Key<T>> it = fetchKeys(options
                                                            .copy()
                                                            .limit(1));
        Key<T> key = (it.hasNext()) ? it.next() : null;
        it.close();
        return key;
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
        n.setQuery(n); // feels weird, correct?
        n.validateName = validateName;
        n.validateType = validateType;
        n.baseQuery = copy(baseQuery);
        n.options = options != null ? options.copy() : null;

        // fields from superclass
        n.setAttachedTo(getAttachedTo());
        n.setChildren(getChildren() == null ? null : new ArrayList<Criteria>(getChildren()));
        return n;
    }

    protected BasicDBObject copy(final DBObject dbObject) {
        return dbObject == null ? null : new BasicDBObject(dbObject.toMap());
    }

    @Override
    @Deprecated
    public Query<T> comment(final String comment) {
        getOptions().modifier("$comment", comment);
        return this;
    }

    @Override
    public FieldEnd<? extends CriteriaContainerImpl> criteria(final String field) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(this, CriteriaJoin.AND);
        add(container);

        return new FieldEndImpl<CriteriaContainerImpl>(this, field, container);
    }

    @Override
    @Deprecated
    public Query<T> disableCursorTimeout() {
        getOptions().noCursorTimeout(true);
        return this;
    }

    @Override
    @Deprecated
    public Query<T> disableSnapshotMode() {
        getOptions().getModifiers().removeField("$snapshot");

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
    @Deprecated
    public Query<T> enableSnapshotMode() {
        getOptions().modifier("$snapshot", true);
        return this;
    }

    @Override
    public Query<T> enableValidation() {
        validateName = true;
        validateType = true;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
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
        if (projection == null || projection.keySet().size() == 0) {
            return null;
        }

        final MappedClass mc = ds.getMapper().getMappedClass(clazz);

        Entity entityAnnotation = mc.getEntityAnnotation();
        final BasicDBObject fieldsFilter = copy(projection);

        if (includeFields && entityAnnotation != null && !entityAnnotation.noClassnameStored()) {
            fieldsFilter.put(Mapper.CLASS_NAME_FIELDNAME, 1);
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
            obj.putAll((BSONObject) baseQuery);
        }

        addTo(obj);

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
        getOptions().modifier("$hint", idxName);
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
            getOptions().modifier("$min", new Document(lowerBound.toMap()));
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<T> maxScan(final int value) {
        if (value > 0) {
            getOptions().modifier("$maxScan", value);
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<T> maxTime(final long value, final TimeUnit unit) {
        getOptions().modifier("$maxTimeMS", MILLISECONDS.convert(value, unit));
        return this;
    }

    long getMaxTime(final TimeUnit unit) {
        Long maxTime = (Long) getOptions().getModifiers().get("$maxTimeMS");
        return unit.convert(maxTime != null ? maxTime : 0, MILLISECONDS);
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
        validateQuery(clazz, ds.getMapper(), new StringBuilder(sort.getField()), FilterOperator.IN, "", false, false);

        getOptions().sort(sort.toDatabase());

        return this;
    }

    @Override
    public Query<T> order(final Sort... sorts) {
        BasicDBObject sortList = new BasicDBObject();
        for (Sort sort : sorts) {
            String s = sort.getField();
            if (validateName) {
                final StringBuilder sb = new StringBuilder(s);
                validateQuery(clazz, ds.getMapper(), sb, FilterOperator.IN, "", true, false);
                s = sb.toString();
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
    @SuppressWarnings("deprecation")
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
        retrievedFields(true, fields.toArray(new String[fields.size()]));
        return this;
    }

    @Override
    public Query<T> project(final String field, final boolean include) {
        final StringBuilder sb = new StringBuilder(field);
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.EQUAL, null, validateName, false);
        String fieldName = sb.toString();
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
        final StringBuilder sb = new StringBuilder(field);
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.EQUAL, null, validateName, false);
        String fieldName = sb.toString();
        validateProjections(fieldName, true);
        project(fieldName, slice.toDatabase());
        return this;
    }

    @Override
    public Query<T> project(final Meta meta) {
        final StringBuilder sb = new StringBuilder(meta.getField());
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.EQUAL, null, false, false);
        String fieldName = sb.toString();
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
    @Deprecated
    public Query<T> returnKey() {
        getOptions().getModifiers().put("$returnKey", true);
        return this;
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
    @Deprecated
    public Query<T> upperIndexBound(final DBObject upperBound) {
        if (upperBound != null) {
            getOptions().getModifiers().put("$max", new BasicDBObject(upperBound.toMap()));
        }

        return this;
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
        return null;
    }

    /**
     * @return the Datastore
     */
    public DatastoreImpl getDatastore() {
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
    public MorphiaIterator<T, T> iterator() {
        return fetch();
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

        if (options.isSnapshot() && (findOptions.getSortDBObject() != null || findOptions.hasHint())) {
            LOG.warning("Snapshotted query should not have hint/sort.");
        }

        if (findOptions.getCursorType() != NonTailable && (findOptions.getSortDBObject() != null)) {
            LOG.warning("Sorting on tail is not allowed.");
        }

        DBCollectionFindOptions options = findOptions.getOptions()
                                                        .sort(getSortObject())
                                                        .projection(getFieldsObject());
        return dbColl.find(query, options)
                     .setDecoderFactory(ds.getDecoderFact());
    }

    @Override
    public String toString() {
        return String.format("{ query: %s %s }", getQueryObject(), getOptions().getProjection() == null
                                                                   ? ""
                                                                   : ", projection: " + getFieldsObject());
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    protected FilterOperator translate(final String operator) {
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
        if (dbOptions.getModifiers() != null ? !dbOptions.getModifiers().equals(that.getModifiers()) : that.getModifiers() != null) {
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
        result = 31 * result + (options.getModifiers() != null ? options.getModifiers().hashCode() : 0);
        result = 31 * result + (options.getProjection() != null ? options.getProjection().hashCode() : 0);
        result = 31 * result + (int) (options.getMaxTime(MILLISECONDS) ^ options.getMaxTime(MILLISECONDS) >>> 32);
        result = 31 * result + (int) (options.getMaxAwaitTime(MILLISECONDS) ^ options.getMaxAwaitTime(MILLISECONDS) >>> 32);
        result = 31 * result + options.getSkip();
        result = 31 * result + (options.getSortDBObject() != null ? options.getSortDBObject().hashCode() : 0);
        result = 31 * result + (options.getCursorType() != null ? options.getCursorType().hashCode() : 0);
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
}
