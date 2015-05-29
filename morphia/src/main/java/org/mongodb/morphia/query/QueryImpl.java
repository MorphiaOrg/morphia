package org.mongodb.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.mongodb.morphia.query.QueryValidator.validateQuery;


/**
 * <p>Implementation of Query</p>
 *
 * @param <T> The type we will be querying for, and returning.
 * @author Scott Hernandez
 */
public class QueryImpl<T> extends CriteriaContainerImpl implements Query<T> {
    private static final Logger LOG = MorphiaLoggerFactory.get(QueryImpl.class);

    private EntityCache cache;
    private boolean validateName = true;
    private boolean validateType = true;

    private String[] fields;
    private Boolean includeFields;
    private BasicDBObject sort;
    private BasicDBObject max;
    private BasicDBObject min;
    private final DatastoreImpl ds;
    private final DBCollection dbColl;
    private int offset;
    private int limit = -1;
    private int batchSize;
    private String indexHint;
    private final Class<T> clazz;
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

    @Override
    public QueryImpl<T> cloneQuery() {
        final QueryImpl<T> n = new QueryImpl<T>(clazz, dbColl, ds);
        n.batchSize = batchSize;
        n.cache = ds.getMapper().createEntityCache(); // fresh cache
        n.fields = fields == null ? null : copy();
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
        n.sort = (BasicDBObject) (sort == null ? null : sort.clone());
        n.max = max;
        n.min = min;
        n.baseQuery = (BasicDBObject) (baseQuery == null ? null : baseQuery.clone());

        // fields from superclass
        n.setAttachedTo(getAttachedTo());
        n.setChildren(getChildren() == null ? null : new ArrayList<Criteria>(getChildren()));
        n.tail = tail;
        n.tailAwaitData = tailAwaitData;
        return n;
    }

    private String[] copy() {
        final String[] copy = new String[fields.length];
        System.arraycopy(fields, 0, copy, 0, fields.length);
        return copy;
    }

    @Override
    public DBCollection getCollection() {
        return dbColl;
    }

    public void setQueryObject(final DBObject query) {
        baseQuery = (BasicDBObject) query;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLimit() {
        return limit;
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

    public DatastoreImpl getDatastore() {
        return ds;
    }

    @Override
    public DBObject getFieldsObject() {
        if (fields == null || fields.length == 0) {
            return null;
        }

        final Map<String, Integer> fieldsFilter = new HashMap<String, Integer>();
        for (String field : fields) {
            final StringBuilder sb = new StringBuilder(field); //validate might modify prop string to translate java field name to db 
            validateQuery(clazz, ds.getMapper(), sb, FilterOperator.EQUAL, null, validateName, false);
            field = sb.toString();
            fieldsFilter.put(field, (includeFields ? 1 : 0));
        }

        final MappedClass mc = ds.getMapper().getMappedClass(clazz);
        
        Entity entityAnnotation = mc.getEntityAnnotation();
        if (includeFields && entityAnnotation != null && !entityAnnotation.noClassnameStored()) {
            fieldsFilter.put(Mapper.CLASS_NAME_FIELDNAME, 1);
        }

        return new BasicDBObject(fieldsFilter);
    }

    @Override
    public DBObject getSortObject() {
        return (sort == null) ? null : sort;
    }

    public boolean isValidatingNames() {
        return validateName;
    }

    public boolean isValidatingTypes() {
        return validateType;
    }

    @Override
    public long countAll() {
        final DBObject query = getQueryObject();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Executing count(" + dbColl.getName() + ") for query: " + query);
        }
        return dbColl.getCount(query);
    }

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

        if (comment != null){
            cursor.addSpecial("$comment", comment);
        }

        if (returnKey){
            cursor.returnKey();
        }

        return cursor;
    }


    @Override
    public MorphiaIterator<T, T> fetch() {
        final DBCursor cursor = prepareCursor();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting cursor(" + dbColl.getName() + ")  for query:" + cursor.getQuery());
        }

        return new MorphiaIterator<T, T>(cursor, ds.getMapper(), clazz, dbColl.getName(), cache);
    }


    @Override
    public MorphiaKeyIterator<T> fetchKeys() {
        final String[] oldFields = fields;
        final Boolean oldInclude = includeFields;
        fields = new String[]{Mapper.ID_KEY};
        includeFields = true;
        final DBCursor cursor = prepareCursor();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Getting cursor(" + dbColl.getName() + ") for query:" + cursor.getQuery());
        }

        fields = oldFields;
        includeFields = oldInclude;
        return new MorphiaKeyIterator<T>(cursor, ds.getMapper(), clazz, dbColl.getName());
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
    public MorphiaIterator<T, T> fetchEmptyEntities() {
        final String[] oldFields = fields;
        final Boolean oldInclude = includeFields;
        fields = new String[]{Mapper.ID_KEY};
        includeFields = true;
        final MorphiaIterator<T, T> res = fetch();
        fields = oldFields;
        includeFields = oldInclude;
        return res;
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    protected FilterOperator translate(final String operator) {
        return FilterOperator.fromString(operator);
    }

    @Override
    public Query<T> filter(final String condition, final Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final String prop = parts[0].trim();
        final FilterOperator op = (parts.length == 2) ? translate(parts[1]) : FilterOperator.EQUAL;

        add(new FieldCriteria(this, prop, op, value, validateName, validateType));

        return this;
    }

    @Override
    public Query<T> where(final CodeWScope js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public Query<T> where(final String js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public Query<T> enableValidation() {
        validateName = true;
        validateType = true;
        return this;
    }

    @Override
    public Query<T> disableValidation() {
        validateName = false;
        validateType = false;
        return this;
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
    public Query<T> limit(final int value) {
        limit = value;
        return this;
    }

    @Override
    public Query<T> batchSize(final int value) {
        batchSize = value;
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
    public Query<T> comment(final String comment) {
        this.comment = comment;
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
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public Query<T> offset(final int value) {
        offset = value;
        return this;
    }


    @Override
    public Query<T> order(final String condition) {
        if (snapshotted) {
            throw new QueryException("order cannot be used on a snapshotted query.");
        }
        sort = parseFieldsString(condition, clazz, ds.getMapper(), validateName);

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
    public Query<T> lowerIndexBound(final DBObject lowerBound) {
        if (lowerBound != null) {
            min = new BasicDBObject(lowerBound.toMap());
        }

        return this;
    }


    /**
     * parses the string and validates each part
     */
    public static BasicDBObject parseFieldsString(final String str, final Class clazz, final Mapper mapper, final boolean validate) {
        BasicDBObjectBuilder ret = BasicDBObjectBuilder.start();
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
            ret = ret.add(s, dir);
        }
        return (BasicDBObject) ret.get();
    }

    @Override
    public MorphiaIterator<T, T> iterator() {
        return fetch();
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
    public Class<T> getEntityClass() {
        return clazz;
    }

    public String toString() {
        return getQueryObject().toString();
    }

    @Override
    public FieldEnd<? extends Query<T>> field(final String name) {
        return field(name, validateName);
    }

    private FieldEnd<? extends Query<T>> field(final String field, final boolean validate) {
        return new FieldEndImpl<QueryImpl<T>>(this, field, this, validate);
    }

    @Override
    public FieldEnd<? extends CriteriaContainerImpl> criteria(final String field) {
        return criteria(field, validateName);
    }

    private FieldEnd<? extends CriteriaContainerImpl> criteria(final String field, final boolean validate) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(this, CriteriaJoin.AND);
        add(container);

        return new FieldEndImpl<CriteriaContainerImpl>(this, field, container, validate);
    }

    //TODO: test this.
    @Override
    public Query<T> hintIndex(final String idxName) {
        indexHint = idxName;
        return this;
    }

    @Override
    public Query<T> retrievedFields(final boolean include, final String... list) {
        if (includeFields != null && include != includeFields) {
            throw new IllegalStateException("You cannot mix include and excluded fields together!");
        }
        includeFields = include;
        fields = list;
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

    /**
     * Enabled snapshotted mode where duplicate results (which may be updated during the lifetime of the cursor) will not be returned. Not
     * compatible with order/sort and hint.
     */
    @Override
    public Query<T> enableSnapshotMode() {
        snapshotted = true;
        return this;
    }

    /**
     * Disable snapshotted mode (default mode). This will be faster but changes made during the cursor may cause duplicates. *
     */
    @Override
    public Query<T> disableSnapshotMode() {
        snapshotted = false;
        return this;
    }

    @Override
    public Query<T> useReadPreference(final ReadPreference readPref) {
        this.readPref = readPref;
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

    /**
     * Disables cursor timeout on server.
     */
    @Override
    public Query<T> disableCursorTimeout() {
        noTimeout = true;
        return this;
    }

    /**
     * Enables cursor timeout on server.
     */
    @Override
    public Query<T> enableCursorTimeout() {
        noTimeout = false;
        return this;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public Map<String, Object> explain() {
        DBCursor cursor = prepareCursor();
        return (BasicDBObject) cursor.explain();
    }
}
