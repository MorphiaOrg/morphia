package dev.morphia.query;

/**
 * Defines the base Criteria implementation.
 */
public abstract class AbstractCriteria implements Criteria {
    private CriteriaContainer attachedTo;

    @Override
    public void attach(final CriteriaContainer container) {
        if (attachedTo != null) {
            attachedTo.remove(this);
        }

        attachedTo = container;
    }

    /**
     * @return the CriteriaContainer this Criteria is attached to
     */
    public CriteriaContainer getAttachedTo() {
        return attachedTo;
    }

    /**
     * Sets the parents CriteriaContainer for this Criteria
     *
     * @param attachedTo the CriteriaContainer
     */
    public void setAttachedTo(final CriteriaContainer attachedTo) {
        this.attachedTo = attachedTo;
    }
}
