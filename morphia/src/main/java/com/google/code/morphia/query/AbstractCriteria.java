package com.google.code.morphia.query;


public abstract class AbstractCriteria implements Criteria {
  protected CriteriaContainerImpl attachedTo;

  public void attach(final CriteriaContainerImpl container) {
    if (attachedTo != null) {
      attachedTo.remove(this);
    }

    attachedTo = container;
  }
}
