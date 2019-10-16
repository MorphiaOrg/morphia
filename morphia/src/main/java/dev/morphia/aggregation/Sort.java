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
}
