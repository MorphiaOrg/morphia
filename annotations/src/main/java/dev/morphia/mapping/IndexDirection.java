package dev.morphia.mapping;

/**
 * Defines the "direction" of an index.
 */
public enum IndexDirection {
    /**
     * ascending
     */
    ASC(1),
    /**
     * descending
     */
    DESC(-1),
    /**
     * geo2d
     */
    GEO2D("2d"),
    /**
     * geo2d sphere
     */
    GEO2DSPHERE("2dsphere");

    private final Object direction;

    IndexDirection(Object o) {
        direction = o;
    }

    /**
     * Returns the value as needed by the index definition document
     *
     * @return the value
     */
    public Object toIndexValue() {
        return direction;
    }
}
