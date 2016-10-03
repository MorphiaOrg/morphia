package org.mongodb.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import org.bson.BSONObject;
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
        FindOptions copy = options.copy();
        copy.limit(1);
        final MorphiaIterator<T, T> it = fetch(copy);
        T t = (it.hasNext()) ? it.next() : null;
        it.close();
        return t;
    }

    @Override
    public Key<T> getKey() {
        return getKey(getOptions());
    }

    @Override
    public Key<T> getKey(final FindOptions options) {
        FindOptions copy = options.copy();
        copy.limit(1);
        final MorphiaIterator<T, Key<T>> it = fetchKeys(copy);
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
        FindOptions copy = getOptions().copy();
        copy.cursorType(awaitData ? TailableAwait : Tailable);
        return fetch(copy);
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
        n.options = getOptions().copy();
        n.cache = ds.getMapper().createEntityCache(); // fresh cache
        n.includeFields = includeFields;
        n.setQuery(n); // feels weird, correct?
        n.validateName = validateName;
        n.validateType = validateType;
        n.baseQuery = copy(baseQuery);

        // fields from superclass
        n.setAttachedTo(getAttachedTo());
        n.setChildren(getChildren() == null ? null : new ArrayList<Criteria>(getChildren()));
        return n;
    }

    protected BasicDBObject copy(final BasicDBObject dbObject) {
        return dbObject == null ? null : new BasicDBObject(dbObject.toMap());
    }

    @Override
    @Deprecated
    public Query<T> comment(final String comment) {
        getOptions().getModifiersDBObject().put("$comment", comment);
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
        getOptions().getModifiersDBObject().remove("$snapshot");

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
        getOptions().getModifiersDBObject().put("$snapshot", true);
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
        return (BasicDBObject) prepareCursor(getOptions()).explain();
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
    public DBCollection getCollection() {
        return dbColl;
    }

    @Override
    public Class<T> getEntityClass() {
        return clazz;
    }

    @Override
    public DBObject getFieldsObject() {
        BasicDBObject projection = (BasicDBObject) getOptions().getProjection();
        if (projection == null || projection.size() == 0) {
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
        baseQuery = (BasicDBObject) query;
    }

    @Override
    public DBObject getSortObject() {
        BasicDBObject sort = (BasicDBObject) getOptions().getSortDBObject();
        return (sort == null) ? null : new BasicDBObject(sort);
    }

    @Override
    @Deprecated
    public Query<T> hintIndex(final String idxName) {
        getOptions().getModifiersDBObject().put("$hint", idxName);
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
    public Query<T> lowerIndexBound(final DBObject lowerBound) {
        if (lowerBound != null) {
            getOptions().getModifiersDBObject().put("$min", new BasicDBObject(lowerBound.toMap()));
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<T> maxScan(final int value) {
        if (value > 0) {
            getOptions().getModifiersDBObject().put("$maxScan", value);
        }
        return this;
    }

    @Override
    @Deprecated
    public Query<T> maxTime(final long value, final TimeUnit unit) {
        getOptions().getModifiersDBObject().put("$maxTimeMS", MILLISECONDS.convert(value, unit));
        return this;
    }

    @Override
    @Deprecated
    public long getMaxTime(final TimeUnit unit) {
        Long maxTime = (Long) getOptions().getModifiersDBObject().get("$maxTimeMS");
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
        if (isSnapshot()) {
            throw new QueryException("order cannot be used on a snapshotted query.");
        }
        getOptions().sort(parseFieldsString(sort, clazz, ds.getMapper(), validateName));

        return this;
    }

    private boolean isSnapshot() {
        Boolean snapshot = (Boolean) getOptions().getModifiersDBObject().get("$snaphot");
        return snapshot != null && snapshot;
    }

    @Override
    public Query<T> order(final Meta sort) {
        if (isSnapshot()) {
            throw new QueryException("order cannot be used on a snapshotted query.");
        }

        final StringBuilder sb = new StringBuilder(sort.getField());
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.IN, "", false, false);

        getOptions().sort(sort.toDatabase());

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
        getOptions().getModifiersDBObject().put("$returnKey", true);
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
            getOptions().getModifiersDBObject().put("$max", new BasicDBObject(upperBound.toMap()));
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


        final DBCursor cursor = dbColl.find(query, findOptions.getOptions()
                                                              .sort(getSortObject())
                                                              .projection(getFieldsObject()));
        cursor.setDecoderFactory(ds.getDecoderFact());
        switch (findOptions.getCursorType()) {
            case TailableAwait:
                cursor.addOption(Bytes.QUERYOPTION_AWAITDATA);
            case Tailable:
                cursor.addOption(Bytes.QUERYOPTION_TAILABLE);
                break;
            default:
                break;
        }

        if (isSnapshot() && (findOptions.getSortDBObject() != null || findOptions.getModifiersDBObject().get("$indexHint") != null)) {
            LOG.warning("Snapshotted query should not have hint/sort.");
        }

        if (findOptions.getCursorType() != NonTailable && (findOptions.getSortDBObject() != null)) {
            LOG.warning("Sorting on tail is not allowed.");
        }

        return cursor;
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
}
