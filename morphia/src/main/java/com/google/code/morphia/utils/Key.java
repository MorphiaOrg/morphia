package com.google.code.morphia.utils;

import java.io.Serializable;

import com.google.code.morphia.Mapper;
import com.mongodb.DBRef;

/**
 * <p>The key object; this class is take from the app-engine datastore (mostly).  
 * It is also Serializable and GWT-safe, enabling your entity objects to 
 * be used for GWT RPC should you so desire.</p>
 * 
 * <p>You may use normal DBRef objects as relationships in your entities if you
 * desire neither type safety nor GWTability.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> (from Objectify code-base)
 * @author Scott Hernandez
 */
public class Key<T> implements Serializable, Comparable<Key<?>>
{
	private static final long serialVersionUID = 1L;
	
	/** 
	 * The name of the class which represents the kind.  As much as
	 * we'd like to use the normal String kind value here, translating
	 * back to a Class for getKind() would then require a link to the
	 * OFactory, making this object non-serializable.
	 */
	protected String kind;
	protected Class<? extends T> kindClass;
	
	/** Null if there is no parent */
	protected Key<?> parent;
	
	/** Either id or name will be valid */
	protected Object id;

	/** For GWT serialization */
	protected Key() {}
	
	/** Create a key with an id */
	public Key(Class<? extends T> kind, Object id)
	{
		this(null, kind, id);
	}
	
	/** Create a key with an id */
	public Key(String kind, Object id)
	{
		this.parent = null;
		this.kind = kind;
		this.id = id;
	}

	/** Create a key with a DBRef*/
	public Key(DBRef ref)
	{
		this.parent = null;
		this.kind = ref.getRef();
		this.id = ref.getId();
	}

	public DBRef toRef() {
		if (kind == null) throw new IllegalStateException("missing collect-name; please call toRef(Mapper)");
		return new DBRef(null, kind, id);
	}
	
	public DBRef toRef(Mapper mapr) {
		if (kind != null) return toRef();
		if (kindClass == null && kind == null) throw new IllegalStateException("missing kindClass; please call toRef(Mapper)");
		kind = mapr.getCollectionName(kindClass);
		return new DBRef(null, kind, id);
	}
	
	/** Create a key with a parent and a long id */
	public Key(Key<?> parent, Class<? extends T> kind, Object id)
	{
		this.parent = parent;
		this.kindClass = kind;
		this.id = id;
	}
	
	/**
	 * @return the id associated with this key.
	 */
	public Object getId()
	{
		return this.id;
	}
	
	/**
	 * @return the collection-name.
	 */
	public String getKind()
	{
		return this.kind;
	}
	
	public Class<? extends T> getKindClass() {
		return this.kindClass;
	}
	
	public String updateKind(Mapper mapr) {
		if (kind == null && kindClass == null) 
			throw new IllegalStateException("Key is invalid! " + toString());
		else if (kind == null) 
			kind = mapr.getMappedClass(kindClass).defCollName;
		
		return kind;
	}
	/**
	 * @return the parent key, or null if there is no parent.  Note that
	 *  the parent could potentially have any type. 
	 */
	@SuppressWarnings("unchecked")
	public <V> Key<V> getParent()
	{
		return (Key<V>)this.parent;
	}

	/**
	 * <p>Compares based on the following traits, in order:</p>
	 * <ol>
	 * <li>kind/kindClass</li>
	 * <li>parent</li>
	 * <li>id or name</li>
	 * </ol>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Key<?> other)
	{
		int cmp = 0;
		// First kind
		if (other.kindClass != null && kindClass != null) {
			cmp = this.kindClass.getName().compareTo(other.kindClass.getName());
			if (cmp != 0)
				return cmp;
		}
		cmp = compareNullable(this.kind, other.kind);
		if (cmp != 0)
			return cmp;

		// Then parent
		cmp = compareNullable(this.parent, other.parent);
		if (cmp != 0)
			return cmp;

		try {
			cmp = compareNullable((Comparable)this.id,(Comparable)other.id);
			if (cmp != 0)
				return cmp;
		} catch (Exception e) {
			//continue
		}

		return 0;
	}

	/** */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		
		if (!(obj instanceof Key<?>))
			return false;
		
		return this.compareTo((Key<?>)obj) == 0;
	}

	/** */
	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}

	/** Creates a human-readable version of this key */
	@Override
	public String toString()
	{
		StringBuilder bld = new StringBuilder("Key{");

		if ( kind != null) {
			bld.append("kind=");
			bld.append(this.kind);
		} else {
			bld.append("kindClass=");
			bld.append(this.kindClass.getName());			
		}
		bld.append(", parent=");
		bld.append(this.parent);
		bld.append(", id=");
		bld.append(this.id);
		bld.append("}");
		
		return bld.toString();
	}
	
	/** */
	@SuppressWarnings("unchecked")
	private static int compareNullable(Comparable o1, Comparable o2)
	{
		if (o1 == null && o2 == null)
			return 0;
		if (o1 == null && o2 != null)
			return -1;
		else if (o1 != null && o2 == null)
			return 1;
		else
			return o1.compareTo(o2);
	}
}