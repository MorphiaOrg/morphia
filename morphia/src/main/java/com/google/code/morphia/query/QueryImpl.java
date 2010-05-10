package com.google.code.morphia.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.DatastoreImpl;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;
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
    private static final Logger log = Logger.getLogger(Mapper.class.getName());

    
	boolean validating = true;
	BasicDBObjectBuilder query = null;
	BasicDBObjectBuilder fields = null;
	BasicDBObjectBuilder sort = null;
	DatastoreImpl ds = null;
	DBCollection dbColl = null;
	int offset = 0;
	int limit = -1;
	Class<T> clazz = null;
	
	public QueryImpl(Class<T> clazz, DBCollection coll, Datastore ds) {
		this.clazz = clazz;
		this.ds = ((DatastoreImpl)ds);
		this.dbColl = coll;
	}
	public QueryImpl(Class<T> clazz, DBCollection coll, Datastore ds, int offset, int limit) {
		this(clazz, coll, ds);
		this.offset = offset;
		this.limit = limit;
	}	

	public DBObject getQueryObject() {
		return (query == null) ? null : query.get(); 
	}
	
	public DBObject getFieldsObject() {
		return (fields == null) ? null : fields.get(); 
	}	
	
	public DBObject getSortObject() {
		return (sort == null) ? null : sort.get(); 
	}	

	@Override
	public long countAll() {
		return dbColl.getCount(getQueryObject());
	}

	@Override
	public Iterable<T> fetch() {
		DBCursor cursor;
		if (offset > 0 || limit > 0) {
			DBObject query = getQueryObject();
			DBObject fields = getFieldsObject();
			Iterator<DBObject> it = dbColl.find(query, fields, offset, limit);
			return new MorphiaIterator<T>(it, ds.getMapper(), clazz, dbColl.getName());
		} else
			cursor = dbColl.find(getQueryObject());
		
		if (sort != null) cursor = cursor.sort(getSortObject());
		
		return new MorphiaIterator<T>(cursor, ds.getMapper(), clazz, dbColl.getName());
	}

	@Override
	public Iterable<Key<T>> fetchKeys() {
		DBCursor cursor;
		if (offset > 0 || limit > 0) {
			DBObject query = getQueryObject();
			DBObject fields = getFieldsObject();
			Iterator<DBObject> it = dbColl.find(query, fields, offset, limit);
			return new MorphiaKeyIterator<T>(it, ds.getMapper(), clazz, dbColl.getName());
		} else
			cursor = dbColl.find(getQueryObject());
		
		if (sort != null) cursor = cursor.sort(getSortObject());
		
		return new MorphiaKeyIterator<T>(cursor, ds.getMapper(), clazz, dbColl.getName());
	}

	@Override
	public List<T> asList() {
		List<T> results = new ArrayList<T>(); 
		for(T ent : fetch()) 
			results.add(ent); 
		return results;
	}

	@Override
	public List<Key<T>> asKeyList() {
		List<Key<T>> results = new ArrayList<Key<T>>(); 
		for(Key<T> key : fetchKeys()) 
			results.add(key); 
		return results;
	}

	@Override
	public Iterable<T> fetchIdsOnly() {
		fields = BasicDBObjectBuilder.start(Mapper.ID_KEY, 1);
		return fetch();
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
		else if (operator.toLowerCase().equals("size"))
			return FilterOperator.SIZE;
		else
			throw new IllegalArgumentException("Unknown operator '" + operator + "'");
	}

	@SuppressWarnings("unchecked")
	@Override
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
		
		if (query == null) query = BasicDBObjectBuilder.start();
		Mapper mapr = ds.getMapper();
		Object mappedValue;
		MappedClass mc = null;
		try {
			if (!ReflectionUtils.isPropertyType(value.getClass()))
				if (mf!=null && !mf.isTypeMongoCompatible())
					mc=mapr.getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubType());
				else
					mc = mapr.getMappedClass(value);
		} catch (Exception e) {
			//Ignore these. It is likely they related to mapping validation that is unimportant for queries (the query will fail/return-empty anyway)
			log.log(Level.FINEST, "Error during mapping filter criteria: " , e);
		}
		
		//convert the value to Key (DBRef) if it is a entity/@Reference of the field type is Key
		if ((mf!=null && (mf.hasAnnotation(Reference.class) || mf.getType().isAssignableFrom(Key.class)))  
				|| (mc != null && mc.getEntityAnnotation() != null)) {
			try {
				Key<?> k = (value instanceof Key) ? (Key<?>)value : ds.getKey(value);
				mappedValue = k.toRef(mapr);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error converting value(" + value + ") to reference.", e);
				mappedValue = mapr.toMongoObject(value);
			}
		}
		else if (mf!=null && mf.hasAnnotation(Serialized.class))
			try {
				mappedValue = mapr.serialize(value); } catch (IOException e) { throw new RuntimeException(e); }
		else
			mappedValue = Mapper.asObjectIdMaybe(mapr.toMongoObject(value));
		
		Class<?> type = mappedValue.getClass();
		
		//convert single values into lists for $in/$nin
		if ((op == FilterOperator.IN || op == FilterOperator.NOT_IN) && 
			!type.isArray() && !ReflectionUtils.implementsAnyInterface(type, Iterable.class, Collection.class, List.class, Set.class, Map.class)) {
			mappedValue = Collections.singletonList(mappedValue);
		}
		
		if (FilterOperator.EQUAL.equals(op))
			query.add(prop, mappedValue);
		else
			query.push(prop).add(op.val(), mappedValue);

		return this;
	}
	
	@Override
	public Query<T> enableValidation(){ validating = true; return this; }
	@Override
	public Query<T> disableValidation(){ validating = false; return this; }

	/** Validate the path, and value type, returning the mappedfield for the field at the path */
	private MappedField validate(String prop, Object value) {
		String[] parts = prop.split("\\.");
//		if (parts.length == 0) parts = new String[]{prop};
		MappedClass mc = ds.getMapper().getMappedClass(this.clazz);
		MappedField mf;
		for(int i=0; ; ) {
			String part = parts[i];
			if(part.equals(Mapper.ID_KEY)) 
				mf = mc.getMappedField(mc.getIdField().getName());
			else
				mf = mc.getMappedField(part);
			
			if (mf == null) {
				mf = mc.getMappedFieldByClassField(part);
				if (mf != null)
					throw new MappingException("The field '" + part + "' is named '" + mf.getName() + "' in '" + this.clazz.getName()+ "' " +
							"(while validating - '" + prop + "'); Please use '" + mf.getName() + "' in your query.");
				else
					throw new MappingException("The field '" + part + "' could not be found in '" + this.clazz.getName()+ "' while validating - " + prop);
			}
			i++;
			if (mf.isMap()) {
				//skip the map key validation, and move to the next part 
				i++;
			}
			if (i >= parts.length) break;
			mc = ds.getMapper().getMappedClass((mf.isSingleValue()) ? mf.getType() : mf.getSubType());
		}
		
		//TODO Do better validation of the data. Maybe check map/list/array types as well.
		if (mf.isSingleValue() && 
			!value.getClass().isAssignableFrom(mf.getType()) &&
			//hack to let Long match long, and so on
			!value.getClass().getSimpleName().toLowerCase().equals(mf.getType().getSimpleName().toLowerCase())) {
			
			if (mf.getSubType() == null || !value.getClass().isAssignableFrom(mf.getSubType())) {
				Throwable t = new Throwable();
				log.warning("Datatypes for the query may be inconsistent; searching with an instance of " 
						+ value.getClass().getName() + " when the field " + mf.getDeclaringClass().getName()+ "." + mf.getClassFieldName() 
						+ " is a " + mf.getType().getName());
				log.log(Level.FINE, "Location of warning:", t);
			}
		}
		
		return mf;
		
	}
	
	@Override
	public T get() {
		int oldLimit = limit;
		limit = 1;
		Iterator<T> it = fetch().iterator();
		limit = oldLimit;		
		return (it.hasNext()) ? it.next() : null ;
	}

	@Override
	public Key<T> getKey() {
		int oldLimit = limit;
		limit = 1;
		Iterator<Key<T>> it = fetchKeys().iterator();
		limit = oldLimit;		
		return (it.hasNext()) ?  it.next() : null;
	}

	@Override
	public Query<T> limit(int value) {
		this.limit = value;
		return this;
	}

	@Override
	public Query<T> offset(int value) {
		this.offset = value;
		return this;
	}

	@Override
	public Query<T> order(String condition) {
		sort = BasicDBObjectBuilder.start();
		String[] sorts = condition.split(",");
		for (int i = 0; i < sorts.length; i++) {
			String s = sorts[i];
			condition = condition.trim();
			int dir = 1;
			
			if (condition.startsWith("-"))
			{
				dir = -1;
				condition = condition.substring(1).trim();
			}
	
			sort = sort.add(s, dir);
		}
		return this;
	}

	@Override
	public Iterator<T> iterator() {
		return fetch().iterator();
	}
	
	public Class<T> getEntityType() {
		return this.clazz;
	}	
	
	public static class FieldPartImpl<T> implements FieldPart<T>{
		
		protected final String fieldExpr;
		protected final QueryImpl<T> query;
		public FieldPartImpl(String fe, QueryImpl<T> q) {this.fieldExpr = fe; this.query=q;}
		@Override
		public Query<T> doesNotExist() {
			query.filter("" + fieldExpr + " exists", 0);
			return query;
		}
		@Override
		public Query<T> equal(Object val) {
			query.filter(fieldExpr + " =", val);
			return query;
		}
		@Override
		public Query<T> exists() {
			query.filter("" + fieldExpr + " exists", true);
			return query;
		}
		@Override
		public Query<T> greaterThan(Object val) {
			query.filter(fieldExpr + " >", val);
			return query;
		}
		@Override
		public Query<T> greaterThanOrEq(Object val) {
			query.filter(fieldExpr + " >=", val);
			return query;
		}
		@Override
		public Query<T> hasThisOne(Object val) {
			query.filter(fieldExpr + " =", val);
			return query;
		}
		@Override
		public Query<T> hasAllOf(Iterable<?> vals) {
			query.filter(fieldExpr + " all", vals);
			return query;
		}
		@Override
		public Query<T> hasAnyOf(Iterable<?> vals) {
			query.filter(fieldExpr + " in", vals);
			return query;
		}
		@Override
		public Query<T> hasNoneOf(Iterable<?> vals) {
			query.filter(fieldExpr + " nin", vals);
			return query;
		}
		@Override
		public Query<T> lessThan(Object val) {
			query.filter(fieldExpr + " <", val);
			return query;
		}
		@Override
		public Query<T> lessThanOrEq(Object val) {
			query.filter(fieldExpr + " <=", val);
			return query;
		}
		@Override
		public Query<T> notEqual(Object val) {
			query.filter(fieldExpr + " <>", val);
			return query;
		}
	}

	public FieldPart<T> field(String fieldExpr) {
		return new FieldPartImpl<T>(fieldExpr, this);
	}

}
