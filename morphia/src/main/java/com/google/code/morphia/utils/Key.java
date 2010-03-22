package com.google.code.morphia.utils;

import java.io.Serializable;

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
	protected String kindClassName;
	
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
		this.kindClassName = kind;
		this.id = id;
	}

	/** Create a key with a DBRef*/
	public Key(DBRef ref)
	{
		this.parent = null;
		this.kindClassName = ref.getRef();
		this.id = ref.getId();
	}

	public DBRef toRef() {
		return new DBRef(null, kindClassName, id);
	}
	/** Create a key with a parent and a long id */
	public Key(Key<?> parent, Class<? extends T> kind, Object id)
	{
		this.parent = parent;
		this.kindClassName = kind.getName();
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
	 * @return the name of the Class associated with this key.
	 */
	public String getKindClassName()
	{
		return this.kindClassName;
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
	 * <li>kind</li>
	 * <li>parent</li>
	 * <li>id or name</li>
	 * </ol>
	 */
	@Override
	public int compareTo(Key<?> other)
	{
		// First kind
		int cmp = this.kindClassName.compareTo(other.kindClassName);
		if (cmp != 0)
			return cmp;

		// Then parent
		cmp = compareNullable(this.parent, other.parent);
		if (cmp != 0)
			return cmp;
		
		//TODO: do something with the ids.
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
		StringBuilder bld = new StringBuilder();
		bld.append("Key{kindClassName=");
		bld.append(this.kindClassName);
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