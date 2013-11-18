package org.mongodb.morphia.aggregation;

public class Sort {
    private final String field;
    private final int direction;

    public Sort(final String field, final int direction) {
        this.field = field;
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    public String getField() {
        return field;
    }

    public static Sort ascending(final String field) {
        return new Sort(field, 1);
    }
}
