package dev.morphia.aggregation.experimental.expressions.impls;

public enum ConvertType {
    DOUBLE("double", 1),
    STRING("string", 2),
    OBJECT_ID("objectId", 7),
    BOOLEAN("bool", 8),
    DATE("date", 9),
    INT("int", 16),
    LONG("long", 18),
    DECIMAL("decimal", 19);

    private final String name;
    private final int identifier;

    ConvertType(String name, int identifier) {
        this.name = name;
        this.identifier = identifier;
    }

    /**
     * @return the numeric identifier of this type
     */
    public int getIdentifier() {
        return identifier;
    }

    /**
     * @return the name of this type
     */
    public String getName() {
        return name;
    }
}