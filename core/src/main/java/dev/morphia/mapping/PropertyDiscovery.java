package dev.morphia.mapping;

/**
 * @deprecated 3.0 will evaluate both field and getter/setters for annotation so this setting becomes vestigial
 */
@Deprecated(since = "2.4", forRemoval = true)
public enum PropertyDiscovery {
    /**
     * look at fields
     */
    FIELDS,
    /**
     * look at methods
     */
    METHODS
}
