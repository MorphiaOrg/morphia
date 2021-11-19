package dev.morphia.query;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Defines the base Criteria implementation.
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public abstract class AbstractCriteria implements Criteria {
    private CriteriaContainer attachedTo;

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void attach(CriteriaContainer container) {
        if (attachedTo != null) {
            attachedTo.remove(this);
        }

        attachedTo = container;
    }
}
