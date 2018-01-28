package org.mongodb.morphia.mapping.classinfo;

/**
 * Signifies that the implementing class <strong>may</strong> use in-memory caching which can be enabled or disabled.
 */
public interface Cacheable {

    /**
     * Enable or disable caching
     *
     * @param caching whether or not to cache
     */
    void setCaching(boolean caching);

    /**
     * @return whether or not the implementing type is currently caching values
     */
    boolean isCaching();

}
