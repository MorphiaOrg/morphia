package dev.morphia.aggregation.expressions.impls;

/**
 * Defines target types for doing type conversions.
 */
public enum ConvertType {
    /**
     * the double type
     */
    DOUBLE("double", 1),
    /**
     * the string type
     */
    STRING("string", 2),
    /**
     * the ObjectId type
     */
    OBJECT_ID("objectId", 7),
    /**
     * the boolean type
     */
    BOOLEAN("bool", 8),
    /**
     * the date type
     */
    DATE("date", 9),
    /**
     * the int type
     */
    INT("int", 16),
    /**
     * the long type
     */
    LONG("long", 18),
    /**
     * the decimal type
     */
    DECIMAL("decimal", 19);

    private final String name;
    private final int identifier;

    ConvertType(String name, int identifier) {
        this.name = name;
        this.identifier = identifier;
    }

    /**
     * @return the name of this type
     */
    public String getName() {
        return name;
    }
}