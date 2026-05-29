package dev.morphia.mapping;

/**
 * Defines the type of the index to create for a field.
 */
public enum IndexType {
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
    GEO2DSPHERE("2dsphere"),
    /**
     * hashed
     */
    HASHED("hashed"),
    /**
     * text
     */
    TEXT("text");

    private final Object type;

    IndexType(Object o) {
        type = o;
    }

    /**
     * Returns the enum instance for the given value
     *
     * @param value the value to find
     * @return the enum instance
     * @since 1.3
     */
    public static IndexType fromValue(Object value) {
        for (IndexType indexType : values()) {
            if (indexType.type.equals(value)) {
                return indexType;
            }
        }
        throw new IllegalArgumentException("No enum value found for " + value);
    }

    /**
     * Returns the value as needed by the index definition document
     *
     * @return the value
     */
    public Object toIndexValue() {
        return type;
    }
}
