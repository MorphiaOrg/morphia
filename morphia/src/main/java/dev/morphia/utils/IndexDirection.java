package dev.morphia.utils;


/**
 * Defines the "direction" of an index.
 */
public enum IndexDirection {
    ASC(1),
    DESC(-1),
    GEO2D("2d"),
    GEO2DSPHERE("2dsphere");

    private final Object direction;

    IndexDirection(final Object o) {
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
