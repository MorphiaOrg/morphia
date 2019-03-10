package dev.morphia.query;

/**
 * Used for sorting query results or defining a sort stage in an aggregation pipeline
 *
 * @mongodb.driver.manual reference/operator/aggregation/sort/ $sort
 * @since 1.3
 */
public class Sort {
    private static final String NATURAL = "$natural";

    private final String field;
    private final int order;

    /**
     * Creates a sort on a field with a direction.
     * <ul>
     * <li>1 to specify ascending order.</li>
     * <li>-1 to specify descending order.</li>
     * </ul>
     *
     * @param field the field
     * @param order the order
     */
    protected Sort(final String field, final int order) {
        this.field = field;
        this.order = order;
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
     * Creates a descending sort on a field
     *
     * @param field the field
     * @return the Sort instance
     */
    public static Sort descending(final String field) {
        return new Sort(field, -1);
    }

    /**
     * Creates an ascending sort on a field
     *
     * @return the Sort instance
     */
    public static Sort naturalAscending() {
        return new Sort(NATURAL, 1);
    }

    /**
     * Creates a descending natural sort on a field
     *
     * @return the Sort instance
     */
    public static Sort naturalDescending() {
        return new Sort(NATURAL, -1);
    }

    /**
     * Returns the sort order.
     * <ul>
     * <li>1 for ascending order.</li>
     * <li>-1 for descending order.</li>
     * </ul>
     *
     * @return the sort order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the sort field
     */
    public String getField() {
        return field;
    }
}
