package com.google.code.morphia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.code.morphia.utils.Key;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@SuppressWarnings("unchecked")
public class QueryImpl<T> implements Query<T> {

	BasicDBObjectBuilder query = null;
	BasicDBObjectBuilder fields = null;
	BasicDBObjectBuilder sort = null;
	Datastore ds = null;
	DBCollection dbColl = null;
	int offset = 0;
	int limit = -1;
	Class clazz = null;
	
	public QueryImpl(Class clazz, DBCollection coll, Datastore ds) {
		this.clazz = clazz;
		this.ds = ds;
		this.dbColl = coll;
	}
	public QueryImpl(Class clazz, DBCollection coll, Datastore ds, int offset, int limit) {
		this(clazz, coll, ds);
		this.offset = offset;
		this.limit = limit;
	}	

	private DBObject getQueryObject() {
		return (query == null) ? null : query.get(); 
	}
	private DBObject getFieldsObject() {
		return (fields == null) ? null : fields.get(); 
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
			return new MorphiaIterator<T>(it, ds.getMorphia().getMapper(), clazz, dbColl.getName());
		} else
			cursor = dbColl.find(getQueryObject());
		
		if (sort != null) cursor = cursor.sort(sort.get());
		
		return new MorphiaIterator<T>(cursor, ds.getMorphia().getMapper(), clazz, dbColl.getName());
	}

	@Override
	public List<T> asList() {
		List<T> results = new ArrayList<T>(); 
		for(T ent : fetch()) 
			results.add(ent); 
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

	@Override
	public Query<T> filter(String condition, Object value) {
		String[] parts = condition.trim().split(" ");
		if (parts.length < 1 || parts.length > 6)
			throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
		
		String prop = parts[0].trim();
		FilterOperator op = (parts.length == 2) ? this.translate(parts[1]) : FilterOperator.EQUAL;

		if (query == null) query = BasicDBObjectBuilder.start();
		Mapper mapr = this.ds.getMorphia().getMapper();
		Object mappedValue = Mapper.asObjectIdMaybe(mapr.objectToValue(value));
		if (FilterOperator.EQUAL.equals(op))
			query.add(prop, mappedValue);
		else
			query.push(prop).add(op.val(), mappedValue);

		return this;
	}
	

	@Override
	public T get() {
		int oldLimit = limit;
		limit=1;
		Iterable<T> it = fetch();
		limit = oldLimit;		
		return (it.iterator().hasNext()) ? it.iterator().next() : null ;
	}

	@Override
	public Key<T> getKey() {
		Iterable<T> it = fetchIdsOnly();
		return (it.iterator().hasNext()) ? new Key<T>(clazz, clazz) : null;
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
}
