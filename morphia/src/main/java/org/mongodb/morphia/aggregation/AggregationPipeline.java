package org.mongodb.morphia.aggregation;


import org.mongodb.morphia.query.MorphiaIterator;

public interface AggregationPipeline<T, U> {
    AggregationPipeline<T, U> project(Projection... projections);
    
    AggregationPipeline<T, U> group(String id, Group... projections);

    MorphiaIterator<U, U> aggregate();
}
