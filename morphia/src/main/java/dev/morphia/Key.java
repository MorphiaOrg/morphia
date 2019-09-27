package dev.morphia;


import java.io.Serializable;
import java.util.Arrays;

/**
 * <p> The key object; this class is take from the app-engine datastore (mostly) implementation. It is also Serializable and GWT-safe,
 * enabling your entity objects to be used for GWT RPC should you so desire. </p> <p/> <p> You may use normal DBRef objects as
 * relationships
 * in your entities if you desire neither type safety nor GWT-ability. </p>
 *
 * @param <T> The type of the entity
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
     *
     * @param type       the type of the entity
     * @param collection the collection in which the entity lives
     * @param id         the value of the entity's ID
     */
    public Key(final Class<? extends T> type, final String collection, final Object id) {
        this.type = type;
        this.collection = collection;
        this.id = id;
    }

    /**
     * Create a key with an id
     *
     * @param type       the type of the entity
     * @param collection the collection in which the entity lives
     * @param idBytes    the value of the entity's ID
     */
    public Key(final Class<? extends T> type, final String collection, final byte[] idBytes) {
        this.type = type;
        this.collection = collection;
        this.idBytes = Arrays.copyOf(idBytes, idBytes.length);
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

    /**
     * @return the collection name.
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Sets the collection name.
     *
     * @param collection the collection to use
     */
    public void setCollection(final String collection) {
        this.collection = collection.intern();
    }

    /**
     * @return the id associated with this key.
     */
    public Object getId() {
        return id;
    }

    /**
     * @return type of the entity
     */
    public Class<? extends T> getType() {
        return type;
    }

    /**
     * Sets the type of the entity for this Key
     *
     * @param clazz the type to use
     */
    public void setType(final Class<? extends T> clazz) {
        type = clazz;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Key<?> && compareTo((Key<T>) obj) == 0;

    }

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

    private void checkState(final Key k) {
        if (k.type == null && k.collection == null) {
            throw new IllegalStateException("Collection must be specified (or a class).");
        }
        if (k.id == null && k.idBytes == null) {
            throw new IllegalStateException("id must be specified");
        }
    }
}
