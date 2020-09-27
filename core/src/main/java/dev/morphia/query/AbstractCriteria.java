package dev.morphia.query;

/**
 * Defines the base Criteria implementation.
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public abstract class AbstractCriteria implements Criteria {
    private CriteriaContainer attachedTo;

    @Override
    public void attach(CriteriaContainer container) {
        if (attachedTo != null) {
            attachedTo.remove(this);
        }

        attachedTo = container;
    }
}
