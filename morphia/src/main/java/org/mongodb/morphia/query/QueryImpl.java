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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
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
    private BasicDBObject projections;
    private Boolean includeFields;
    private BasicDBObject sort;
    private BasicDBObject max;
    private BasicDBObject min;
    private int offset;
    private int limit = -1;
    private int batchSize;
    private String indexHint;
    private BasicDBObject baseQuery;
    private boolean snapshotted;
    private boolean noTimeout;
    private boolean tail;
    private boolean tailAwaitData;
    private ReadPreference readPref;
    private Integer maxScan;
    private Long maxTime;
    private TimeUnit maxTimeUnit;
    private String comment;
    private boolean returnKey;

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
            readPref = this.ds.getMapper().getMappedClass(clazz).getEntityAnnotation().queryNonPrimary()
                       ? ReadPreference.secondaryPreferred()
                       : null;
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
        final List<Key<T>> results = new ArrayList<Key<T>>();
        MorphiaKeyIterator<T> keys = fetchKeys();
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
        final List<T> results = new ArrayList<T>();
        final MorphiaIterator<T, T> iter = fetch();
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
    public long countAll() {
        final DBObject query = getQueryObject();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing count(" + dbColl.getName() + ") for query: " + query);
        }
        return dbColl.getCount(query);
    }

    @Override
    public MorphiaIterator<T, T> fetch() {
        final DBCursor cursor = prepareCursor();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting cursor(" + dbColl.getName() + ")  for query:" + cursor.getQuery());
        }

        return new MorphiaIterator<T, T>(ds, cursor, ds.getMapper(), clazz, dbColl.getName(), cache);
    }

    @Override
    public MorphiaIterator<T, T> fetchEmptyEntities() {
        QueryImpl<T> cloned = cloneQuery();
        cloned.projections = new BasicDBObject(Mapper.ID_KEY, 1);
        cloned.includeFields = true;
        return cloned.fetch();
    }

    @Override
    public MorphiaKeyIterator<T> fetchKeys() {
        QueryImpl<T> cloned = cloneQuery();
        cloned.projections = new BasicDBObject(Mapper.ID_KEY, 1);
        cloned.includeFields = true;

        return new MorphiaKeyIterator<T>(ds, cloned.prepareCursor(), ds.getMapper(), clazz, dbColl.getName());
    }

    @Override
    public T get() {
        final int oldLimit = limit;
        limit = 1;
        final Iterator<T> it = fetch().iterator();
        limit = oldLimit;
        return (it.hasNext()) ? it.next() : null;
    }

    @Override
    public Key<T> getKey() {
        final int oldLimit = limit;
        limit = 1;
        final Iterator<Key<T>> it = fetchKeys().iterator();
        limit = oldLimit;
        return (it.hasNext()) ? it.next() : null;
    }

    @Override
    public MorphiaIterator<T, T> tail() {
        return tail(true);
    }

    @Override
    public MorphiaIterator<T, T> tail(final boolean awaitData) {
        //Create a new query for this, so the current one is not affected.
        final QueryImpl<T> tailQ = cloneQuery();
        tailQ.tail = true;
        tailQ.tailAwaitData = awaitData;
        return tailQ.fetch();
    }

    @Override
    public Query<T> batchSize(final int value) {
        batchSize = value;
        return this;
    }

    @Override
    public QueryImpl<T> cloneQuery() {
        final QueryImpl<T> n = new QueryImpl<T>(clazz, dbColl, ds);
        n.batchSize = batchSize;
        n.cache = ds.getMapper().createEntityCache(); // fresh cache
        n.includeFields = includeFields;
        n.indexHint = indexHint;
        n.limit = limit;
        n.noTimeout = noTimeout;
        n.setQuery(n); // feels weird, correct?
        n.offset = offset;
        n.readPref = readPref;
        n.snapshotted = snapshotted;
        n.validateName = validateName;
        n.validateType = validateType;
        n.max = max;
        n.min = min;
        n.projections = copy(projections);
        n.sort = copy(sort);
        n.baseQuery = copy(baseQuery);

        // fields from superclass
        n.setAttachedTo(getAttachedTo());
        n.setChildren(getChildren() == null ? null : new ArrayList<Criteria>(getChildren()));
        n.tail = tail;
        n.tailAwaitData = tailAwaitData;
        return n;
    }

    protected BasicDBObject copy(final BasicDBObject dbObject) {
        return dbObject == null ? null : new BasicDBObject(dbObject.toMap());
    }

    @Override
    public Query<T> comment(final String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public Query<T> disableCursorTimeout() {
        noTimeout = true;
        return this;
    }

    @Override
    public Query<T> disableSnapshotMode() {
        snapshotted = false;
        return this;
    }

    @Override
    public Query<T> disableValidation() {
        validateName = false;
        validateType = false;
        return this;
    }

    @Override
    public Query<T> enableCursorTimeout() {
        noTimeout = false;
        return this;
    }

    @Override
    public Query<T> enableSnapshotMode() {
        snapshotted = true;
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
        DBCursor cursor = prepareCursor();
        return (BasicDBObject) cursor.explain();
    }

    @Override
    public FieldEnd<? extends Query<T>> field(final String name) {
        return field(name, validateName);
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
    public int getBatchSize() {
        return batchSize;
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
        if (projections == null || projections.size() == 0) {
            return null;
        }

        final MappedClass mc = ds.getMapper().getMappedClass(clazz);


        Entity entityAnnotation = mc.getEntityAnnotation();
        final BasicDBObject fieldsFilter = copy(projections);

        if (includeFields && entityAnnotation != null && !entityAnnotation.noClassnameStored()) {
            fieldsFilter.put(Mapper.CLASS_NAME_FIELDNAME, 1);
        }

        return fieldsFilter;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
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
        return (sort == null) ? null : new BasicDBObject(sort);
    }

    @Override
    public Query<T> hintIndex(final String idxName) {
        indexHint = idxName;
        return this;
    }

    @Override
    public Query<T> limit(final int value) {
        limit = value;
        return this;
    }

    @Override
    public Query<T> lowerIndexBound(final DBObject lowerBound) {
        if (lowerBound != null) {
            min = new BasicDBObject(lowerBound.toMap());
        }

        return this;
    }

    @Override
    public Query<T> maxScan(final int value) {
        maxScan = value > 0 ? value : null;
        return this;
    }

    @Override
    public Query<T> maxTime(final long value, final TimeUnit timeUnitValue) {
        maxTime = value > 0 ? value : null;
        maxTimeUnit = timeUnitValue;
        return this;
    }

    @Override
    public Query<T> offset(final int value) {
        offset = value;
        return this;
    }

    @Override
    public Query<T> order(final String sort) {
        if (snapshotted) {
            throw new QueryException("order cannot be used on a snapshotted query.");
        }
        this.sort = parseFieldsString(sort, clazz, ds.getMapper(), validateName
                                     );

        return this;
    }

    @Override
    public Query<T> order(final Meta sort) {
        if (snapshotted) {
            throw new QueryException("order cannot be used on a snapshotted query.");
        }
        final StringBuilder sb = new StringBuilder(sort.getField());
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.IN, "", false, false);
        this.sort = (BasicDBObject) sort.toDatabase();

        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Query<T> queryNonPrimary() {
        readPref = ReadPreference.secondary();
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Query<T> queryPrimaryOnly() {
        readPref = ReadPreference.primary();
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
        projections.put(fieldName, include ? 1 : 0);
        return this;
    }

    @Override
    public Query<T> project(final String field, final ArraySlice slice) {
        final StringBuilder sb = new StringBuilder(field);
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.EQUAL, null, validateName, false);
        String fieldName = sb.toString();
        validateProjections(fieldName, true);
        projections.put(fieldName, slice.toDatabase());
        return this;
    }

    @Override
    public Query<T> project(final Meta meta) {
        final StringBuilder sb = new StringBuilder(meta.getField());
        validateQuery(clazz, ds.getMapper(), sb, FilterOperator.EQUAL, null, false, false);
        String fieldName = sb.toString();
        validateProjections(fieldName, true);
        projections.putAll(meta.toDatabase());
        return this;

    }

    private void validateProjections(final String field, final boolean include) {
        if (includeFields != null && include != includeFields) {
            if (!includeFields || !"_id".equals(field)) {
                throw new ValidationException("You cannot mix included and excluded fields together");
            }
        }
        if (projections == null) {
            projections = new BasicDBObject();
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
    public Query<T> returnKey() {
        this.returnKey = true;
        return this;
    }

    @Override
    public Query<T> search(final String search) {

        final BasicDBObject op = new BasicDBObject("$search", search);

        this.criteria("$text", false).equal(op);

        return this;
    }

    @Override
    public Query<T> search(final String search, final String language) {

        final BasicDBObject op = new BasicDBObject("$search", search)
                                     .append("$language", language);

        this.criteria("$text", false).equal(op);

        return this;
    }

    @Override
    public Query<T> upperIndexBound(final DBObject upperBound) {
        if (upperBound != null) {
            max = new BasicDBObject(upperBound.toMap());
        }

        return this;
    }

    @Override
    public Query<T> useReadPreference(final ReadPreference readPref) {
        this.readPref = readPref;
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
    public FieldEnd<? extends CriteriaContainerImpl> criteria(final String field) {
        return criteria(field, validateName);
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
     */
    public DBCursor prepareCursor() {
        final DBObject query = getQueryObject();
        final DBObject fields = getFieldsObject();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Running query(" + dbColl.getName() + ") : " + query + ", fields:" + fields + ",off:" + offset + ",limit:" + limit);
        }

        final DBCursor cursor = dbColl.find(query, fields);
        cursor.setDecoderFactory(ds.getDecoderFact());

        if (offset > 0) {
            cursor.skip(offset);
        }
        if (limit > 0) {
            cursor.limit(limit);
        }
        if (batchSize != 0) {
            cursor.batchSize(batchSize);
        }
        if (snapshotted) {
            cursor.snapshot();
        }
        if (sort != null) {
            cursor.sort(sort);
        }
        if (indexHint != null) {
            cursor.hint(indexHint);
        }

        if (null != readPref) {
            cursor.setReadPreference(readPref);
        }

        if (noTimeout) {
            cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        }

        if (tail) {
            cursor.addOption(Bytes.QUERYOPTION_TAILABLE);
            if (tailAwaitData) {
                cursor.addOption(Bytes.QUERYOPTION_AWAITDATA);
            }
        }

        //Check for bad options.
        if (snapshotted && (sort != null || indexHint != null)) {
            LOG.warning("Snapshotted query should not have hint/sort.");
        }

        if (tail && (sort != null)) {
            // i don´t think that just warning is enough here, i´d favor a RTE, agree?
            LOG.warning("Sorting on tail is not allowed.");
        }

        if (maxScan != null) {
            cursor.addSpecial("$maxScan", maxScan);
        }

        if (maxTime != null && maxTimeUnit != null) {
            cursor.maxTime(maxTime, maxTimeUnit);
        }

        if (max != null) {
            cursor.addSpecial("$max", max);
        }

        if (min != null) {
            cursor.addSpecial("$min", min);
        }

        if (comment != null) {
            cursor.addSpecial("$comment", comment);
        }

        if (returnKey) {
            cursor.returnKey();
        }

        return cursor;
    }

    @Override
    public String toString() {
        return String.format("{ query: %s %s }", getQueryObject(), projections == null ? "" : ", projection: " + getFieldsObject());
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    protected FilterOperator translate(final String operator) {
        return FilterOperator.fromString(operator);
    }

    private FieldEnd<? extends Query<T>> field(final String field, final boolean validate) {
        return new FieldEndImpl<QueryImpl<T>>(this, field, this, validate);
    }

    FieldEnd<? extends CriteriaContainerImpl> criteria(final String field, final boolean validate) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(this, CriteriaJoin.AND);
        add(container);

        return new FieldEndImpl<CriteriaContainerImpl>(this, field, container, validate);
    }
}
