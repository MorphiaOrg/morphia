package org.mongodb.morphia.query;

/**
 * Internal class to represent groups of {@link Criteria} instances via $and and $or query clauses 
 */
public interface CriteriaContainer extends Criteria {
  void add(Criteria... criteria);

  CriteriaContainer and(Criteria... criteria);

  CriteriaContainer or(Criteria... criteria);

  FieldEnd<? extends CriteriaContainer> criteria(String field);
}
