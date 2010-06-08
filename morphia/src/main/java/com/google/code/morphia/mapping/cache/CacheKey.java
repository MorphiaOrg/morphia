/**
 * 
 */
package com.google.code.morphia.mapping.cache;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public abstract class CacheKey {
	final String ns;
	final Object id;
	
	protected CacheKey(String ns, Object id) {
		this.ns = ns;
		this.id = id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ns == null) ? 0 : ns.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheKey other = (CacheKey) obj;
		if (ns == null) {
			if (other.ns != null)
				return false;
		} else if (!ns.equals(other.ns))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CacheKey [ns=" + ns + ", id=" + id + "]";
	}

}
