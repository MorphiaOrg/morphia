package org.mongodb.morphia.aggregation;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoCursor;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.MorphiaIterator;

import java.util.ArrayList;
import java.util.List;

public class AggregationPipelineImpl<T, U> implements AggregationPipeline<T, U> {
    private final DBCollection collection;
    private final Class<T> source;
    private final Class<U> target;
    private final List<DBObject> stages = new ArrayList<DBObject>();
    private final Mapper mapper;

    public AggregationPipelineImpl(final DatastoreImpl datastore, final Class<T> source, final Class<U> target) {
        this.collection = datastore.getCollection(source);
        mapper = datastore.getMapper();
        this.source = source;
        this.target = target;
    }

    public DBObject toDBObject(final Projection projection) {
        MappedField field = mapper.getMappedClass(source).getMappedField(projection.getSourceField());
        String sourceFieldName = field.getNameToStore();
        if (!stages.isEmpty()) {
            sourceFieldName = "$" + sourceFieldName;
        }
        if (projection.getProjection() != null) {
            return new BasicDBObject(projection.getProjectedField(), toDBObject(projection.getProjection()));
        } else if (projection.getProjectedField() != null) {
            return new BasicDBObject(sourceFieldName, projection.getProjectedField());
        } else {
            return new BasicDBObject(field.getNameToStore(), projection.isSuppressed() ? 0 : 1);
        }
    }
    
    public AggregationPipeline<T, U> project(final Projection... projections) {
        BasicDBObject proj = new BasicDBObject();
        for (Projection projection : projections) {
            proj.putAll(toDBObject(projection));
        }
        stages.add(new BasicDBObject("$project", proj));
        return this;
    }

    public MorphiaIterator<U, U> aggregate() {
        System.out.println("stages = " + stages);
        MongoCursor cursor = collection.aggregate(stages, AggregationOptions.builder().build());
        return new MorphiaIterator<U, U>(cursor, mapper, target, collection.getName(), mapper.createEntityCache());
    }
}
