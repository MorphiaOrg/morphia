package com.google.code.morphia.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.DatastoreImpl;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.logging.MorphiaLogger;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.Serializer;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.utils.Assert;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * <p>Implementation of Query</p>
 * 
 * @author Scott Hernandez
 *
 * @param <T> The type we will be querying for, and returning.
 */
public class QueryImpl<T> implements Query<T> {
	private static final MorphiaLogger log = MorphiaLoggerFactory.get(Mapper.class);
	
	private final EntityCache cache;
	private boolean validating = true;
	private Map<String, Object> query = null;
	private String[] fields = null;
	private Boolean includeFields = null;
	private BasicDBObjectBuilder sort = null;
	private DatastoreImpl ds = null;
	private DBCollection dbColl = null;
	private int offset = 0;
	private int limit = -1;
	private String indexHint;
	private Class<T> clazz = null;
	
	public QueryImpl(Class<T> clazz, DBCollection coll, Datastore ds) {
		this.clazz = clazz;
		this.ds = ((DatastoreImpl)ds);
		this.dbColl = coll;
		this.cache = this.ds.getMapper().createEntityCache();
	}
	
	public QueryImpl(Class<T> clazz, DBCollection coll, Datastore ds, int offset, int limit) {
		this(clazz, coll, ds);
		this.offset = offset;
		this.limit = limit;
	}

	@SuppressWarnings("unchecked")
	public void setQueryObject(DBObject query) {
		this.query = (Map<String, Object>) query;
	}
	
	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}
	
	public DBObject getQueryObject() {
		return (query == null) ? null : new BasicDBObject(query);
	}
	
	public DBObject getFieldsObject() {
		if (fields == null || fields.length == 0) 
			return null;
		
		Map<String, Boolean> fieldsFilter = new HashMap<String, Boolean>();
		for(String field : this.fields)
			fieldsFilter.put(field, (includeFields));
		
		return new BasicDBObject(fieldsFilter);
	}
	
	public DBObject getSortObject() {
		return (sort == null) ? null : sort.get();
	}
	

	public long countAll() {
		return dbColl.getCount(getQueryObject());
	}
	
	public DBCursor prepareCursor() {
		DBObject query = getQueryObject();
		DBObject fields = getFieldsObject();
		
		if (log.isDebugEnabled())
			log.debug("Running query: " + query + ", " + fields + ",off:" + offset + ",limit:" + limit);
		
		DBCursor cursor = dbColl.find(query, fields);
		if (offset > 0)
			cursor.skip(offset);
		if (limit > 0)
			cursor.limit(limit);
		if (sort != null)
			cursor.sort(getSortObject());
		if (indexHint != null)
			cursor.hint(indexHint);
		
		return cursor;
	}
	

	public Iterable<T> fetch() {
		DBCursor cursor = prepareCursor();
		return new MorphiaIterator<T>(cursor, ds.getMapper(), clazz, dbColl.getName(), cache);
	}
	

	public Iterable<Key<T>> fetchKeys() {
		String[] oldFields = fields;
		Boolean oldInclude = includeFields;
		fields = new String[] {Mapper.ID_KEY};
		includeFields = true;
		DBCursor cursor = prepareCursor();
		fields = oldFields;
		includeFields = oldInclude;
		return new MorphiaKeyIterator<T>(cursor, ds.getMapper(), clazz, dbColl.getName());
	}
	

	public List<T> asList() {
		List<T> results = new ArrayList<T>();
		for(T ent : fetch())
			results.add(ent);

		if (log.isTraceEnabled())
			log.trace("\nasList: " + dbColl.getName() + "\n result size " + results.size() + "\n cache: "
				+ (cache.stats()) + "\n for " + ((query != null) ? new BasicDBObject(query) : "{}"));

		return results;
	}
	

	public List<Key<T>> asKeyList() {
		List<Key<T>> results = new ArrayList<Key<T>>();
		for(Key<T> key : fetchKeys())
			results.add(key);
		return results;
	}
	

	public Iterable<T> fetchEmptyEntities() {
		String[] oldFields = fields;
		Boolean oldInclude = includeFields;
		fields = new String[] {Mapper.ID_KEY};
		includeFields = true;
		Iterable<T> res = fetch();
		fields = oldFields;
		includeFields = oldInclude;
		return res;
	}
	
	/**
	 * Converts the textual operator (">", "<=", etc) into a FilterOperator.
	 * Forgiving about the syntax; != and <> are NOT_EQUAL, = and == are EQUAL.
	 */
	protected FilterOperator translate(String operator)
	{
		operator = operator.trim();
		
		if (operator.equals("=") || operator.equals("=="))
			return FilterOperator.EQUAL;
		else if (operator.equals(">"))
			return FilterOperator.GREATER_THAN;
		else if (operator.equals(">="))
			return FilterOperator.GREATER_THAN_OR_EQUAL;
		else if (operator.equals("<"))
			return FilterOperator.LESS_THAN;
		else if (operator.equals("<="))
			return FilterOperator.LESS_THAN_OR_EQUAL;
		else if (operator.equals("!=") || operator.equals("<>"))
			return FilterOperator.NOT_EQUAL;
		else if (operator.toLowerCase().equals("in"))
			return FilterOperator.IN;
		else if (operator.toLowerCase().equals("nin"))
			return FilterOperator.NOT_IN;
		else if (operator.toLowerCase().equals("all"))
			return FilterOperator.ALL;
		else if (operator.toLowerCase().equals("exists"))
			return FilterOperator.EXISTS;
		else if (operator.toLowerCase().equals("elem"))
			return FilterOperator.ELEMENT_MATCH;
		else if (operator.toLowerCase().equals("size"))
			return FilterOperator.SIZE;
		else
			throw new IllegalArgumentException("Unknown operator '" + operator + "'");
	}
	
	@SuppressWarnings("unchecked")
	public Query<T> filter(String condition, Object value) {
		String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 6)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
		
		String prop = parts[0].trim();
		FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;
		
		//The field we are filtering on, in the java object
		MappedField mf = null;
		if (validating)
			mf = validate(prop, value);
		
		//TODO differentiate between the key/value for maps; we will just get the mf for the field, not which part we are looking for
		
		if (query == null) query = new HashMap<String, Object>();
		Mapper mapr = ds.getMapper();
		Object mappedValue;
		MappedClass mc = null;
		try {
			if (value != null && !ReflectionUtils.isPropertyType(value.getClass()) && !ReflectionUtils.implementsInterface(value.getClass(), Iterable.class))
				if (mf!=null && !mf.isTypeMongoCompatible())
					mc=mapr.getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubType());
				else
					mc = mapr.getMappedClass(value);
		} catch (Exception e) {
			//Ignore these. It is likely they related to mapping validation that is unimportant for queries (the query will fail/return-empty anyway)
			log.debug("Error during mapping filter criteria: ", e);
		}
		
		//convert the value to Key (DBRef) if it is a entity/@Reference or the field type is Key
		if ((mf!=null && (mf.hasAnnotation(Reference.class) || mf.getType().isAssignableFrom(Key.class)))
				|| (mc != null && mc.getEntityAnnotation() != null)) {
			try {
				Key<?> k = (value instanceof Key) ? (Key<?>)value : ds.getKey(value);
				mappedValue = k.toRef(mapr);
			} catch (Exception e) {
				log.debug("Error converting value(" + value + ") to reference.", e);
				mappedValue = mapr.toMongoObject(value);
			}
		}
		else if (mf!=null && mf.hasAnnotation(Serialized.class))
			try {
				mappedValue = Serializer.serialize(value, !mf.getAnnotation(Serialized.class).disableCompression());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		else
			mappedValue = mapr.toMongoObject(value);
		
		Class<?> type = (mappedValue != null) ? mappedValue.getClass() : null;
		
		//convert single values into lists for $in/$nin
		if (type != null && (op == FilterOperator.IN || op == FilterOperator.NOT_IN) && !type.isArray() && !ReflectionUtils.implementsAnyInterface(type, Iterable.class) ) {
			mappedValue = Collections.singletonList(mappedValue);
		}
		
		if (FilterOperator.EQUAL.equals(op))
			query.put(prop, mappedValue); // no operator, prop equals value
		else {
			Object inner = query.get(prop); // operator within inner object
			if (!(inner instanceof Map)) {
				inner = new HashMap<String, Object>();
				query.put(prop, inner);
			}
			((Map<String, Object>)inner).put(op.val(), mappedValue);
		}
		return this;	
	}

    protected Query<T> filterWhere(Object obj){
		if (query == null) {
            query = new HashMap<String, Object>();
        }
        query.put(FilterOperator.WHERE.val(), obj);
        return this;
    }

    public Query<T> where(String js) {
    	return filterWhere(js);
    }

    public Query<T> where(CodeWScope cws) {
    	return filterWhere(cws);
    }
	

	public Query<T> enableValidation(){ validating = true; return this; }

	public Query<T> disableValidation(){ validating = false; return this; }
	
	/** Validate the path, and value type, returning the mappedfield for the field at the path */
	private MappedField validate(String prop, Object value) {
		String[] parts = prop.split("\\.");
		//		if (parts.length == 0) parts = new String[]{prop};
		if (this.clazz == null) return null;
		MappedClass mc = ds.getMapper().getMappedClass(this.clazz);
		MappedField mf;
		for(int i=0; ; ) {
			String part = parts[i];
			mf = mc.getMappedField(part);
			
			if (mf == null) {
				mf = mc.getMappedFieldByJavaField(part);
				if (mf != null)
					throw new QueryException("The field '" + part + "' is named '" + mf.getNameToStore() + "' in '" + this.clazz.getName()+ "' " +
							"(while validating - '" + prop + "'); Please use '" + mf.getNameToStore() + "' in your query.");
				else
					throw new QueryException("The field '" + part + "' could not be found in '" + this.clazz.getName()+ "' while validating - " + prop);
			}
			i++;
			if (mf.isMap()) {
				//skip the map key validation, and move to the next part
				i++;
			}
			//catch people trying to search into @Reference/@Serialized fields
			if (i < parts.length && !canQueryPast(mf))
				throw new QueryException("Can not use dot-notation past '" + part + "' could not be found in '" + this.clazz.getName()+ "' while validating - " + prop);
			
			if (i >= parts.length) break;
			mc = ds.getMapper().getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubType());
		}
		
		if (	 (mf.isSingleValue() && !isCompatibleForQuery(mf.getType(), value)) || 
				((mf.isMultipleValues() && !isCompatibleForQuery(mf.getSubType(), value)))) {
				
			Throwable t = new Throwable();
			log.warning("Datatypes for the query may be inconsistent; searching with an instance of "
					+ value.getClass().getName() + " when the field " + mf.getDeclaringClass().getName()+ "." + mf.getJavaFieldName()
					+ " is a " + mf.getType().getName());
			log.debug("Location of warning:\r\n", t);
		}
		
		return mf;
	}
	
	/** Returns if the MappedField is a Reference or Serilized  */
	public static boolean canQueryPast(MappedField mf) {
		return !(mf.hasAnnotation(Reference.class) || mf.hasAnnotation(Serialized.class));
	}
	
	public static boolean isCompatibleForQuery(Class<?> type, Object value) {
		if (value == null || type == null) 
			return true;
		else if (value instanceof Integer && (int.class.equals(type) || long.class.equals(type) || Long.class.equals(type)))
			return true;
		else if ((value instanceof Integer || value instanceof Long) && (double.class.equals(type) || Double.class.equals(type)))
			return true;
		else if (value instanceof Pattern && String.class.equals(type))
			return true;
		else if (value instanceof List)
			return true;
		else if (value instanceof ObjectId && String.class.equals(type))
			return true;
		else if (!value.getClass().isAssignableFrom(type) &&
				//hack to let Long match long, and so on
				!value.getClass().getSimpleName().toLowerCase().equals(type.getSimpleName().toLowerCase())) {
			return false;
		}
		return true;
	}

	public T get() {
		int oldLimit = limit;
		limit = 1;
		Iterator<T> it = fetch().iterator();
		limit = oldLimit;
		return (it.hasNext()) ? it.next() : null ;
	}
	

	public Key<T> getKey() {
		int oldLimit = limit;
		limit = 1;
		Iterator<Key<T>> it = fetchKeys().iterator();
		limit = oldLimit;
		return (it.hasNext()) ?  it.next() : null;
	}
	

	public Query<T> limit(int value) {
		this.limit = value;
		return this;
	}
	

	public Query<T> skip(int value) {
		this.offset = value;
		return this;
	}

	public Query<T> offset(int value) {
		this.offset = value;
		return this;
	}
	

	public Query<T> order(String condition) {
		sort = BasicDBObjectBuilder.start();
		String[] sorts = condition.split(",");
		for (String s : sorts) {
			s = s.trim();
			int dir = 1;
			
			if (s.startsWith("-"))
			{
				dir = -1;
				s = s.substring(1).trim();
			}
			
			sort = sort.add(s, dir);
		}
		return this;
	}
	

	public Iterator<T> iterator() {
		return fetch().iterator();
	}
	
	public Class<T> getEntityClass() {
		return this.clazz;
	}
	
	public static class QueryFieldEndImpl<T> implements QueryFieldEnd<T>{
		
		protected final String fieldExpr;
		protected final QueryImpl<T> query;
		public QueryFieldEndImpl(String fe, QueryImpl<T> q) {this.fieldExpr = fe; this.query=q;}

		public Query<T> startsWith(String prefix) {
			Assert.parametersNotNull("prefix",prefix);
			query.filter("" + fieldExpr, Pattern.compile("^" + prefix));
			return query;
		}
		
		public Query<T> startsWithIgnoreCase(String prefix) {
			Assert.parametersNotNull("prefix", prefix);
			query.filter("" + fieldExpr, Pattern.compile("^" + prefix, Pattern.CASE_INSENSITIVE));
			return query;
		}
		
		public Query<T> endsWith(String suffix) {
			Assert.parametersNotNull("suffix", suffix);
			query.filter("" + fieldExpr, Pattern.compile(suffix + "$"));
			return query;
		}
		
		public Query<T> endsWithIgnoreCase(String suffix) {
			Assert.parametersNotNull("suffix", suffix);
			query.filter("" + fieldExpr, Pattern.compile(suffix + "$", Pattern.CASE_INSENSITIVE));
			return query;
		}

		public Query<T> contains(String chars) {
			Assert.parametersNotNull("chars", chars);
			query.filter("" + fieldExpr, Pattern.compile(chars));
			return query;
		}
		
		public Query<T> containsIgnoreCase(String chars) {
			Assert.parametersNotNull("chars", chars);
			query.filter("" + fieldExpr, Pattern.compile(chars, Pattern.CASE_INSENSITIVE));
			return query;
		}
		
		public Query<T> doesNotExist() {
			query.filter("" + fieldExpr + " exists", 0);
			return query;
		}

		public Query<T> equal(Object val) {
			query.filter(fieldExpr + " =", val);
			return query;
		}

		public Query<T> exists() {
			query.filter("" + fieldExpr + " exists", true);
			return query;
		}

		public Query<T> greaterThan(Object val) {
			Assert.parametersNotNull("val",val);
			query.filter(fieldExpr + " >", val);
			return query;
		}

		public Query<T> greaterThanOrEq(Object val) {
			Assert.parametersNotNull("val",val);
			query.filter(fieldExpr + " >=", val);
			return query;
		}

		public Query<T> hasThisOne(Object val) {
			query.filter(fieldExpr + " =", val);
			return query;
		}

		public Query<T> hasAllOf(Iterable<?> vals) {
			Assert.parametersNotNull("vals",vals);
			Assert.parameterNotEmpty(vals,"vals");
			query.filter(fieldExpr + " all", vals);
			return query;
		}

		public Query<T> hasAnyOf(Iterable<?> vals) {
			Assert.parametersNotNull("vals",vals);
			Assert.parameterNotEmpty(vals,"vals");
			query.filter(fieldExpr + " in", vals);
			return query;
		}

		public Query<T> hasThisElement(Object val) {
			Assert.parametersNotNull("val",val);
			query.filter(fieldExpr + " elem", val);
			return query;
		}

		public Query<T> hasNoneOf(Iterable<?> vals) {
			Assert.parametersNotNull("vals",vals);
			Assert.parameterNotEmpty(vals,"vals");
			query.filter(fieldExpr + " nin", vals);
			return query;
		}

		public Query<T> lessThan(Object val) {
			Assert.parametersNotNull("val",val);
			query.filter(fieldExpr + " <", val);
			return query;
		}

		public Query<T> lessThanOrEq(Object val) {
			Assert.parametersNotNull("val",val);
			query.filter(fieldExpr + " <=", val);
			return query;
		}

		public Query<T> notEqual(Object val) {
			query.filter(fieldExpr + " <>", val);
			return query;
		}

		public Query<T> sizeEq(int val) {
			Assert.parametersNotNull("val",val);
			query.filter(fieldExpr + " size", val);
			return query;
		}
	}
	
	public QueryFieldEnd<T> field(String fieldExpr) {
		return new QueryFieldEndImpl<T>(fieldExpr, this);
	}

	//TODO: make me work!
	public Query<T> hintIndex(String idxName) {
		return null;
	}

	public Query<T> retrievedFields(boolean include, String...fields){
		if (includeFields != null && include != includeFields)
			throw new IllegalStateException("You cannot mix include and excluded fields together!");
		this.includeFields = include;
		this.fields = fields;
		return this;
	}
}
