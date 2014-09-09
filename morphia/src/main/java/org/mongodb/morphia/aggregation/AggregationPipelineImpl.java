package org.mongodb.morphia.aggregation;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;

public class AggregationPipelineImpl<T, U> implements AggregationPipeline<T, U> {
    private static final Logger LOG = MorphiaLoggerFactory.get(AggregationPipelineImpl.class);

    private final DBCollection collection;
    private final Class<T> source;
    private final List<DBObject> stages = new ArrayList<DBObject>();
    private final Mapper mapper;
    private final DatastoreImpl datastore;
    private boolean firstStage = false;

    public AggregationPipelineImpl(final DatastoreImpl datastore, final Class<T> source) {
        this.datastore = datastore;
        this.collection = datastore.getCollection(source);
        mapper = datastore.getMapper();
        this.source = source;
    }

    @SuppressWarnings("unchecked")
    public DBObject toDBObject(final Projection projection) {
        String sourceFieldName;
        if (firstStage) {
            MappedField field = mapper.getMappedClass(source).getMappedField(projection.getSourceField());
            sourceFieldName = field.getNameToStore();
        } else {
            sourceFieldName = projection.getSourceField();
        }

        if (projection.getProjections() != null) {
            List<Projection> list = projection.getProjections();
            DBObject projections = new BasicDBObject();
            for (Projection subProjection : list) {
                projections.putAll(toDBObject(subProjection));
            }
            return new BasicDBObject(sourceFieldName, projections);
        } else if (projection.getProjectedField() != null) {
            return new BasicDBObject(sourceFieldName, projection.getProjectedField());
        } else {
            return new BasicDBObject(sourceFieldName, projection.isSuppressed() ? 0 : 1);
        }
    }

    public AggregationPipeline<T, U> project(final Projection... projections) {
        firstStage = stages.isEmpty();
        DBObject dbObject = new BasicDBObject();
        for (Projection projection : projections) {
            dbObject.putAll(toDBObject(projection));
        }
        stages.add(new BasicDBObject("$project", dbObject));
        return this;
    }

    public AggregationPipeline<T, U> group(final String id, final Group... groupings) {
        DBObject group = new BasicDBObject("_id", "$" + id);
        for (Group grouping : groupings) {
            Accumulator accumulator = grouping.getAccumulator();
            group.put(grouping.getName(), new BasicDBObject(accumulator.getOperation(), accumulator.getField()));
        }

        stages.add(new BasicDBObject("$group", group));
        return this;
    }

    public AggregationPipeline<T, U> group(final List<Group> id, final Group... groupings) {
        DBObject idGroup = new BasicDBObject();
        for (Group group : id) {
            idGroup.put(group.getName(), group.getSourceField());
        }
        DBObject group = new BasicDBObject("_id", idGroup);
        for (Group grouping : groupings) {
            Accumulator accumulator = grouping.getAccumulator();
            group.put(grouping.getName(), new BasicDBObject(accumulator.getOperation(), accumulator.getField()));
        }

        stages.add(new BasicDBObject("$group", group));
        return this;
    }

    public AggregationPipeline<T, U> match(final Query query) {
        stages.add(new BasicDBObject("$match", query.getQueryObject()));
        return this;
    }

    public AggregationPipeline<T, U> sort(final Sort... sorts) {
        DBObject sortList = new BasicDBObject();
        for (Sort sort : sorts) {
            sortList.put(sort.getField(), sort.getDirection());
        }

        stages.add(new BasicDBObject("$sort", sortList));
        return this;
    }

    public AggregationPipeline<T, U> limit(final int count) {
        stages.add(new BasicDBObject("$limit", count));
        return this;
    }

    public AggregationPipeline<T, U> skip(final int count) {
        stages.add(new BasicDBObject("$skip", count));
        return this;
    }

    public AggregationPipeline<T, U> unwind(final String field) {
        stages.add(new BasicDBObject("$unwind", "$" + field));
        return this;
    }

    public AggregationPipeline<T, U> geoNear(final GeoNear geoNear) {
        DBObject geo = new BasicDBObject();
        putIfNull(geo, "near", geoNear.getNear());
        putIfNull(geo, "distanceField", geoNear.getDistanceField());
        putIfNull(geo, "limit", geoNear.getLimit());
        putIfNull(geo, "num", geoNear.getMaxDocuments());
        putIfNull(geo, "maxDistance", geoNear.getMaxDistance());
        if (geoNear.getQuery() != null) {
            geo.put("query", geoNear.getQuery().getQueryObject());
        }
        putIfNull(geo, "spherical", geoNear.getSpherical());
        putIfNull(geo, "distanceMultiplier", geoNear.getDistanceMultiplier());
        putIfNull(geo, "includeLocs", geoNear.getIncludeLocations());
        putIfNull(geo, "uniqueDocs", geoNear.getUniqueDocuments());
        stages.add(new BasicDBObject("$geoNear", geo));

        return this;
    }

    private void putIfNull(final DBObject dbObject, final String name, final Object value) {
        if (value != null) {
            dbObject.put(name, value);
        }
    }

    @Override
    public MorphiaIterator<U, U> out(final Class<U> target) {
        return out(datastore.getCollection(target).getName(), target);
    }

    @Override
    public MorphiaIterator<U, U> out(final Class<U> target, final AggregationOptions options) {
        return out(datastore.getCollection(target).getName(), target, options);
    }

    @Override
    public MorphiaIterator<U, U> out(final String collectionName, final Class<U> target) {
        return out(collectionName, target, AggregationOptions.builder().build());
    }

    @Override
    public MorphiaIterator<U, U> out(final String collectionName, final Class<U> target, final AggregationOptions options) {
        stages.add(new BasicDBObject("$out", collectionName));
        return aggregate(target, options);
    }

    @Override
    public MorphiaIterator<U, U> aggregate(final Class<U> target) {
        return aggregate(target, AggregationOptions.builder().build(), collection.getReadPreference());
    }

    @Override
    public MorphiaIterator<U, U> aggregate(final Class<U> target, final AggregationOptions options) {
       return aggregate(target, options, collection.getReadPreference());
    }
    
    @Override
    public MorphiaIterator<U, U> aggregate(final Class<U> target, final AggregationOptions options, final ReadPreference readPreference) {
        return aggregate(datastore.getCollection(target).getName(), target, options, readPreference);
    }
    
    @Override
    public MorphiaIterator<U, U> aggregate(final String collectionName, final Class<U> target, final AggregationOptions options, 
                                           final ReadPreference readPreference) {
        LOG.debug("stages = " + stages);

        Cursor cursor = collection.aggregate(stages, options, readPreference);
        return new MorphiaIterator<U, U>(cursor, mapper, target, collection.getName(), mapper.createEntityCache());
    }
}
