package org.mongodb.morphia.mapping.classinfo;

/**
 * A strategy interface for mapping from class id to type; may use in-memory caching and obeys {@link Cacheable}
 *
 * The following expression must <strong>always</strong> return true for any non-null result of {@code getId}
 *
 * <pre> val.getClass() == getClass(getId(val)) </pre>
 */
public interface ClassIdMapper extends Cacheable {

    /**
     * Get a class id for the type of the provided object.
     *
     * If returned, guaranteed to obey the above contract
     *
     * @param value the object for which a type id is desired
     * @return the id for the type or null if it cannot be produced
     */
    String getId(Object value);

    /**
     * Returns a class for the provided ID if can be loaded.
     *
     * @param id the id to load
     * @param <T> the expected class (or superclass); provided for casting convenience.
     * @return the loaded class if found; otherwise null.
     */
    <T> Class<T> getClass(String id);

}
