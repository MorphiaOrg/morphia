package dev.morphia.query;

/**
 * Internal class to represent groups of {@link Criteria} instances via $and and $or query clauses
 */
public interface CriteriaContainer extends Criteria {
    /**
     * Adds Criteria to this container
     *
     * @param criteria the criteria to add
     */
    void add(Criteria... criteria);

    /**
     * Ands Criteria with this CriteriaContainer.
     *
     * @param criteria the criteria
     * @return the container
     */
    CriteriaContainer and(Criteria... criteria);

    /**
     * Creates a criteria against a field
     *
     * @param field the field
     * @return the FieldEnd to define the criteria to apply
     */
    FieldEnd<? extends CriteriaContainer> criteria(String field);

    /**
     * Ors Criteria with this CriteriaContainer.
     *
     * @param criteria the criteria
     * @return the container
     */

    CriteriaContainer or(Criteria... criteria);

    /**
     * Removes Criteria to this container
     *
     * @param criteria the criteria to remove
     */
    void remove(Criteria criteria);
}
