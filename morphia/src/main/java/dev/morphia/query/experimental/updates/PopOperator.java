package dev.morphia.query.experimental.updates;

/**
 * Defines the $pop update operator.
 *
 * @morphia.internal
 * @since 2.0
 */
public class PopOperator extends UpdateOperator {
    /**
     * @param field the field
     * @morphia.internal
     */
    public PopOperator(String field) {
        super("$pop", field, 1);
    }

    /**
     * Remove the first element rather than the last.
     *
     * @return this
     */
    public PopOperator removeFirst() {
        value(-1);
        return this;
    }
}
