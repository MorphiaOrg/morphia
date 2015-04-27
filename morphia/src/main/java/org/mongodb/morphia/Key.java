package org.mongodb.morphia;


import java.io.Serializable;
import java.util.Arrays;

/**
 * <p> The key object; this class is take from the app-engine datastore (mostly) implementation. It is also Serializable and GWT-safe,
 * enabling your entity objects to be used for GWT RPC should you so desire. </p> <p/> <p> You may use normal DBRef objects as relationships
 * in your entities if you desire neither type safety nor GWT-ability. </p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org> (from Objectify codebase)
 * @author Scott Hernandez (adapted to morphia/mongodb)
 */
public class Key<T> implements Serializable, Comparable<Key<T>> {
    private String collection;
    private Class<? extends T> type;

    /**
     * Id value
     */
    private Object id;
    private byte[] idBytes;

    /**
     * For GWT serialization
     */
    protected Key() {
    }

    /**
     * Create a key with an id
     */
    public Key(final Class<? extends T> type, final String collection, final Object id) {
        this.type = type;
        this.collection = collection;
        this.id = id;
    }

    /**
     * Create a key with an id
     */
    public Key(final Class<? extends T> type, final String collection, final byte[] idBytes) {
        this.type = type;
        this.collection = collection;
        this.idBytes = Arrays.copyOf(idBytes, idBytes.length);
    }

    /**
     * @return the id associated with this key.
     */
    public Object getId() {
        return id;
    }

    /**
     * @return the collection-name.
     */
    public String getCollection() {
        return collection;
    }

    /**
     * sets the collection-name.
     */
    public void setCollection(final String collection) {
        this.collection = collection.intern();
    }

    public void setType(final Class<? extends T> clazz) {
        type = clazz;
    }

    public Class<? extends T> getType() {
        return type;
    }

    private void checkState(final Key k) {
        if (k.type == null && k.collection == null) {
            throw new IllegalStateException("Collection must be specified (or a class).");
        }
        if (k.id == null && k.idBytes == null) {
            throw new IllegalStateException("id must be specified");
        }
    }

    /**
     * <p> Compares based on the following traits, in order: </p> 
     * <ol>
     *     <li>collection/type</li>
     *     <li>parent</li> 
     *     <li>id or name</li>
     *  </ol>
     */
    @Override
    public int compareTo(final Key<T> other) {
        checkState(this);
        checkState(other);

        int cmp;
        // First collection
        if (other.type != null && type != null) {
            cmp = type.getName().compareTo(other.type.getName());
            if (cmp != 0) {
                return cmp;
            }
        }
        cmp = compareNullable(collection, other.collection);
        if (cmp != 0) {
            return cmp;
        }

        try {
            cmp = compareNullable((Comparable<?>) id, (Comparable<?>) other.id);
            if (cmp != 0) {
                return cmp;
            }
        } catch (Exception e) {
            // Not a comparable, use equals and String.compareTo().
            cmp = id.equals(other.id) ? 0 : 1;
            if (cmp != 0) {
                return id.toString().compareTo(other.id.toString());
            }
        }

        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Key<?> && compareTo((Key<T>) obj) == 0;

    }

    /** */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Creates a human-readable version of this key
     */
    @Override
    public String toString() {
        final StringBuilder bld = new StringBuilder("Key{");

        if (collection != null) {
            bld.append("collection=");
            bld.append(collection);
        } else {
            bld.append("type=");
            bld.append(type.getName());
        }
        bld.append(", id=");
        bld.append(id);
        bld.append("}");

        return bld.toString();
    }

    /** */
    @SuppressWarnings("unchecked")
    private static int compareNullable(final Comparable o1, final Comparable o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }
}