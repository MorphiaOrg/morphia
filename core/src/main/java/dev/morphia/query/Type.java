package dev.morphia.query;

/**
 * Defines BSON types for use in querying against field types.
 *
 * @author suresh chaudhari
 */
public enum Type {

    DOUBLE(0X01),

    STRING(0X02),

    OBJECT(0X03),

    ARRAY(0X04),

    BINARY_DATA(0X05),

    UNDEFINED(0X06),

    OBJECT_ID(0X07),

    BOOLEAN(0X08),

    DATE(0X09),

    NULL(0X0A),

    REGULAR_EXPRESSION(0X0B),

    JAVASCRIPT(0X0D),

    SYMBOL(0X0E),

    JAVASCRIPT_WITH_SCOPE(0X0F),

    INTEGER_32_BIT(0X10),

    TIMESTAMP(0X11),

    INTEGER_64_BIT(0X12),

    MIN_KEY(0XFF),

    MAX_KEY(0X7F);

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
