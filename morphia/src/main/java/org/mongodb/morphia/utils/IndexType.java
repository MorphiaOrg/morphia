package org.mongodb.morphia.utils;

/**
 * Defines the type of the index to create for a field.
 */
public enum IndexType {
    ASC(1),
    DESC(-1),
    GEO2D("2d"),
    GEO2DSPHERE("2dsphere"),
    TEXT("text");

    private final Object direction;

    IndexType(final Object o) {
        direction = o;
    }

    public Object toIndexValue() {
        return direction;
    }
}
