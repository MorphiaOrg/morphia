package org.mongodb.morphia.aggregation;


public interface Aggregation<T, U> {
  Aggregation<T, U> project(Projection<T, U>... projections);
  
  Projection<T, U> projection(String field);
  
  Projection<T, U> projection(String target, Projection<T, U>... projections);
  
}
