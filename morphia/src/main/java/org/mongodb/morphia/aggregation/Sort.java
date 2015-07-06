package org.mongodb.morphia.aggregation;

/**
 * Defines a sort stage in an aggregation pipeline
 *
 * @mongodb.driver.manual reference/operator/aggregation/sort/ $sort
 */
public class Sort {
    private final String field;
    private final int direction;

    /**
     * Creates a sort on a field with a direction.
     * <ul>
     * <li>1 to specify ascending order.</li>
     * <li>-1 to specify descending order.</li>
     * </ul>
     *
     * @param field     the field
     * @param direction the direction
     */
    public Sort(final String field, final int direction) {
        this.field = field;
        this.direction = direction;
    }

    /**
     * Creates an ascending sort on a field
     *
     * @param field the field
     * @return the Sort instance
     */
    public static Sort ascending(final String field) {
        return new Sort(field, 1);
    }

    /**
     * @return the sort direction
     */
    public int getDirection() {
        return direction;
    }

    /**
     * @return the sort field
     */
    public String getField() {
        return field;
    }
}
