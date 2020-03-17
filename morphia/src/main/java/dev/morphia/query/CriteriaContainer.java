package dev.morphia.query;

import static dev.morphia.query.MorphiaQuery.legacyOperation;

/**
 * Internal class to represent groups of {@link Criteria} instances via $and and $or query clauses
 *
 * @deprecated
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public interface CriteriaContainer extends Criteria {
    /**
     * Adds Criteria to this container
     *
     * @param criteria the criteria to add
     */
    default void add(Criteria... criteria) {
        legacyOperation();
    }

    /**
     * Ands Criteria with this CriteriaContainer.
     *
     * @param criteria the criteria
     * @return the container
     */
    default CriteriaContainer and(Criteria... criteria) {
        return legacyOperation();
    }

    /**
     * Creates a criteria against a field
     *
     * @param field the field
     * @return the FieldEnd to define the criteria to apply
     */
    default FieldEnd<? extends CriteriaContainer> criteria(String field) {
        return legacyOperation();
    }

    /**
     * Ors Criteria with this CriteriaContainer.
     *
     * @param criteria the criteria
     * @return the container
     */

    default CriteriaContainer or(Criteria... criteria) {
        return legacyOperation();
    }

    /**
     * Removes Criteria to this container
     *
     * @param criteria the criteria to remove
     */
    default void remove(Criteria criteria) {
        legacyOperation();
    }
}
