package org.mongodb.morphia.query;


public abstract class AbstractCriteria implements Criteria {
    private CriteriaContainerImpl attachedTo;

    public void attach(final CriteriaContainerImpl container) {
        if (attachedTo != null) {
            attachedTo.remove(this);
        }

        attachedTo = container;
    }

    public CriteriaContainerImpl getAttachedTo() {
        return attachedTo;
    }

    public void setAttachedTo(final CriteriaContainerImpl attachedTo) {
        this.attachedTo = attachedTo;
    }
}
