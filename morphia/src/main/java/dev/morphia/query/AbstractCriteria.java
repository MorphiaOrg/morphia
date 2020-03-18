package dev.morphia.query;

/**
 * Defines the base Criteria implementation.
 */
@SuppressWarnings("removal")
public abstract class AbstractCriteria implements Criteria {
    private CriteriaContainer attachedTo;

    @Override
    public void attach(final CriteriaContainer container) {
        if (attachedTo != null) {
            attachedTo.remove(this);
        }

        attachedTo = container;
    }
}
