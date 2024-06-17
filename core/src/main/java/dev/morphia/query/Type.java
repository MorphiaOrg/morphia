package dev.morphia.query;

/**
 * Defines BSON types for use in querying against field types.
 */
public enum Type {

    /**
     * double
     */
    DOUBLE(1),

    /**
     * string
     */
    STRING(2),

    /**
     * object
     */
    OBJECT(3),

    /**
     * array
     */
    ARRAY(4),

    /**
     * binData
     */
    BINARY_DATA(5),

    /**
     * undefined
     */
    UNDEFINED(6),

    /**
     * ObjectId
     */
    OBJECT_ID(7),

    /**
     * boolean
     */
    BOOLEAN(8),

    /**
     * date
     */
    DATE(9),

    /**
     * null
     */
    NULL(10),

    /**
     * regex
     */
    REGULAR_EXPRESSION(11),

    /**
     * DBPointer
     */
    DB_POINTER(12),

    /**
     * javascript
     */
    JAVASCRIPT(13),

    /**
     * symbol
     */
    SYMBOL(14),

    /**
     * javascript with scope
     */
    JAVASCRIPT_WITH_SCOPE(15),

    /**
     * int32
     */
    INTEGER_32_BIT(16),

    /**
     * timestamp
     */
    TIMESTAMP(17),

    /**
     * int64
     */
    INTEGER_64_BIT(18),

    /**
     * Decimal128
     */
    DECIMAL_128(19),

    /**
     * minKey
     */
    MIN_KEY(255),

    /**
     * maxKey
     */
    MAX_KEY(127);

    private final int value;

    Type(int value) {
        this.value = value;
    }

    /**
     * @return the BSON type value
     */
    public int val() {
        return value;
    }
}
