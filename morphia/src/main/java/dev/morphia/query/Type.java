package dev.morphia.query;

/**
 * Defines BSON types for use in querying against field types.
 *
 * @author suresh chaudhari
 */
public enum Type {

    DOUBLE(1),

    STRING(2),

    OBJECT(3),

    ARRAY(4),

    BINARY_DATA(5),

    UNDEFINED(6),

    OBJECT_ID(7),

    BOOLEAN(8),

    DATE(9),

    NULL(10),

    REGULAR_EXPRESSION(11),

    JAVASCRIPT(13),

    SYMBOL(14),

    JAVASCRIPT_WITH_SCOPE(15),

    INTEGER_32_BIT(16),

    TIMESTAMP(17),

    INTEGER_64_BIT(18),

    MIN_KEY(255),

    MAX_KEY(127);

    private final int value;

    Type(final int value) {
        this.value = value;
    }

    /**
     * @return the BSON type value
     */
    public int val() {
        return value;
    }
}
