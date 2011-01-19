package com.google.code.morphia;

import java.io.Serializable;

/**
 * gwt client impl. 
 * @see com.google.code.morphia.Key
 * @author Scott Hernandez (adapted to morphia/mongodb)
 */
public class Key<T> implements Serializable, Comparable<Key<?>> {
	private static final long serialVersionUID = 1L;
	
	/**
	 * The name of the class which represents the kind.  As much as
	 * we'd like to use the normal String kind value here, translating
	 * back to a Class for getKind() would then require a link to the
	 * OFactory, making this object non-serializable.
	 */
	protected String kind;
	
	/** Id value */
	protected Serializable id;
	protected byte[] idBytes;
	
	/** For GWT serialization */
	protected Key() {}
	
	/** Create a key with an id */
	public Key(String kind, Serializable id)
	{
		this.kind = kind;
		this.id = id;
	}
	
	/**
	 * @return the id associated with this key.
	 */
	public Serializable getId()
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
	/**
	 * sets the collection-name.
	 */
	public void setKind(String newKind) {
		kind = newKind;
	}
	
	private void checkState(Key k) {
		if (k.kind == null)
			throw new IllegalStateException("Kind must be specified (or a class).");
		if (k.id == null && k.idBytes == null)
			throw new IllegalStateException("id must be specified");
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
	public int compareTo(Key<?> other)
	{
		checkState(this);
		checkState(other);
		
		int cmp = 0;
		// First kind
		cmp = compareNullable(this.kind, other.kind);
		if (cmp != 0)
			return cmp;
		
		try {
			cmp = compareNullable((Comparable<?>)this.id,(Comparable<?>)other.id);
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
		} 
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