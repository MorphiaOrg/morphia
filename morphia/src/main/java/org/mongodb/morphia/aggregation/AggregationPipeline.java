package org.mongodb.morphia.aggregation;


import com.mongodb.AggregationOptions;
import com.mongodb.ReadPreference;
import org.mongodb.morphia.query.MorphiaIterator;

import java.util.List;

public interface AggregationPipeline<T, U> {
    AggregationPipeline<T, U> project(Projection... projections);
    
    AggregationPipeline<T, U> group(String id, Group... groupings);
    
    AggregationPipeline<T, U> group(List<Group> id, Group... groupings);

    AggregationPipeline<T, U> match(Matcher... criterion);

    AggregationPipeline<T, U> sort(Sort... sorts);

    MorphiaIterator<U, U> aggregate();
    
    MorphiaIterator<U, U> aggregate(AggregationOptions options);

    MorphiaIterator<U, U> aggregate(ReadPreference readPreference);
    
    MorphiaIterator<U, U> aggregate(AggregationOptions options, ReadPreference readPreference);
}
