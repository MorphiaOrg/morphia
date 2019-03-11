package dev.morphia.aggregation;

/**
 * Defines a sort stage in an aggregation pipeline
 *
 * @mongodb.driver.manual reference/operator/aggregation/sort/ $sort
 * @deprecated Use {@link dev.morphia.query.Sort} instead.
 */
@Deprecated
public class Sort extends dev.morphia.query.Sort {

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
        super(field, direction);
    }

    /**
     * Creates an ascending sort on a field
     *
     * @param field the field
     * @return the Sort instance
     * @deprecated Use {@link dev.morphia.query.Sort#ascending(String)} instead.
     */
    @Deprecated
    public static Sort ascending(final String field) {
        return new Sort(field, 1);
    }

    /**
     * Creates a descending sort on a field
     *
     * @param field the field
     * @return the Sort instance
     * @deprecated Use {@link dev.morphia.query.Sort#descending(String)} instead.
     */
    @Deprecated
    public static Sort descending(final String field) {
        return new Sort(field, -1);
    }

    /**
     * @return the sort direction
     * @deprecated Use {@link dev.morphia.query.Sort#getOrder()} instead.
     */
    @Deprecated
    public int getDirection() {
        return super.getOrder();
    }
}
