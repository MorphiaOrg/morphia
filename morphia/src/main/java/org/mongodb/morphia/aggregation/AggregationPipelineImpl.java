package org.mongodb.morphia.aggregation;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.geo.GeometryShapeConverter;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of an AggregationPipeline.
 */
public class AggregationPipelineImpl implements AggregationPipeline {
    private static final Logger LOG = MorphiaLoggerFactory.get(AggregationPipelineImpl.class);

    private final DBCollection collection;
    private final Class source;
    private final List<DBObject> stages = new ArrayList<DBObject>();
    private final Mapper mapper;
    private final DatastoreImpl datastore;

    /**
     * Creates an AggregationPipeline
     *
     * @param datastore the datastore to use
     * @param source    the source type to aggregate
     */
    public AggregationPipelineImpl(final DatastoreImpl datastore, final Class source) {
        this.datastore = datastore;
        this.collection = datastore.getCollection(source);
        mapper = datastore.getMapper();
        this.source = source;
    }

    /**
     * Returns the internal list of stages for this pipeline.  This is an internal method intended only for testing and validation.  Use
     * at your own risk.
     *
     * @return the list of stages
     */
    public List<DBObject> getStages() {
        return stages;
    }

    @Override
    public <U> Iterator<U> aggregate(final Class<U> target) {
        return aggregate(target, AggregationOptions.builder().build(), collection.getReadPreference());
    }

    @Override
    public <U> Iterator<U> aggregate(final Class<U> target, final AggregationOptions options) {
        return aggregate(target, options, collection.getReadPreference());
    }

    @Override
    public <U> Iterator<U> aggregate(final Class<U> target, final AggregationOptions options, final ReadPreference readPreference) {
        return aggregate(datastore.getCollection(target).getName(), target, options, readPreference);
    }

    @Override
    public <U> Iterator<U> aggregate(final String collectionName, final Class<U> target, final AggregationOptions options,
                                     final ReadPreference readPreference) {
        LOG.debug("stages = " + stages);

        Cursor cursor = collection.aggregate(stages, options, readPreference);
        return new MorphiaIterator<U, U>(datastore, cursor, mapper, target, collectionName, mapper.createEntityCache());
    }

    @Override
    @SuppressWarnings("deprecation")
    public AggregationPipeline geoNear(final GeoNear geoNear) {
        DBObject geo = new BasicDBObject();
        GeometryShapeConverter.PointConverter pointConverter = new GeometryShapeConverter.PointConverter();
        pointConverter.setMapper(mapper);

        putIfNull(geo, "near", geoNear.getNearAsDBObject(pointConverter));
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

    @Override
    public AggregationPipeline group(final Group... groupings) {
        return group((String) null, groupings);
    }

    @Override
    public AggregationPipeline group(final String id, final Group... groupings) {
        DBObject group = new BasicDBObject();
        group.put("_id", id != null ? "$" + id : null);
        for (Group grouping : groupings) {
            group.putAll(grouping.toDBObject());
        }

        stages.add(new BasicDBObject("$group", group));
        return this;
    }

    @Override
    public AggregationPipeline group(final List<GroupElement> id, final Group... groupings) {
        DBObject idGroup = null;
        if (id != null) {
            idGroup = new BasicDBObject();
            for (GroupElement group : id) {
                idGroup.putAll(group.toDBObject());
            }
        }
        DBObject group = new BasicDBObject("_id", idGroup);
        for (Group grouping : groupings) {
            group.putAll(grouping.toDBObject());
        }

        stages.add(new BasicDBObject("$group", group));
        return this;
    }

    @Override
    public AggregationPipeline limit(final int count) {
        stages.add(new BasicDBObject("$limit", count));
        return this;
    }

    @Override
    public AggregationPipeline lookup(final String from, final String localField, final String foreignField, final String as) {
        stages.add(new BasicDBObject("$lookup", new BasicDBObject("from", from)
            .append("localField", localField)
            .append("foreignField", foreignField)
            .append("as", as)));
        return this;
    }

    @Override
    public AggregationPipeline match(final Query query) {
        stages.add(new BasicDBObject("$match", query.getQueryObject()));
        return this;
    }

    @Override
    public <U> Iterator<U> out(final Class<U> target) {
        return out(datastore.getCollection(target).getName(), target);
    }

    @Override
    public <U> Iterator<U> out(final Class<U> target, final AggregationOptions options) {
        return out(datastore.getCollection(target).getName(), target, options);
    }

    @Override
    public <U> Iterator<U> out(final String collectionName, final Class<U> target) {
        return out(collectionName, target, AggregationOptions.builder().build());
    }

    @Override
    public <U> Iterator<U> out(final String collectionName, final Class<U> target, final AggregationOptions options) {
        stages.add(new BasicDBObject("$out", collectionName));
        return aggregate(target, options);
    }

    @Override
    public AggregationPipeline project(final ProjectionElement... projections) {
        boolean firstStage = stages.isEmpty();
        DBObject dbObject = new BasicDBObject();
        for (ProjectionElement projectionElement : projections) {
            if (projectionElement instanceof Projection) {
                Projection projection = (Projection) projectionElement;
                if (firstStage) {
                    MappedField field = mapper.getMappedClass(source).getMappedField(projection.getTarget());
                    projection.setMappedFieldName(field != null ? field.getNameToStore() : projection.getTarget());
                }
            }
            dbObject.putAll(projectionElement.toDBObject());
        }
        stages.add(new BasicDBObject("$project", dbObject));
        return this;
    }

    @Override
    public AggregationPipeline skip(final int count) {
        stages.add(new BasicDBObject("$skip", count));
        return this;
    }

    @Override
    public AggregationPipeline sort(final SortElement... sorts) {
        DBObject sortList = new BasicDBObject();
        for (SortElement sort : sorts) {
            sortList.putAll(sort.toDBObject());
        }

        stages.add(new BasicDBObject("$sort", sortList));
        return this;
    }

    @Override
    public AggregationPipeline unwind(final String field) {
        stages.add(new BasicDBObject("$unwind", "$" + field));
        return this;
    }

    private void putIfNull(final DBObject dbObject, final String name, final Object value) {
        if (value != null) {
            dbObject.put(name, value);
        }
    }

    @Override
    public String toString() {
        return stages.toString();
    }
}
