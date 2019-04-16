package dev.morphia.aggregation;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.UnwindOptions;
import dev.morphia.query.BucketAutoOptions;
import dev.morphia.query.BucketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.morphia.geo.GeometryShapeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.internal.MorphiaCursor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of an AggregationPipeline.
 */
@SuppressWarnings("deprecation")
public class AggregationPipelineImpl implements AggregationPipeline {
    private static final Logger LOG = LoggerFactory.getLogger(AggregationPipelineImpl.class);

    private final DBCollection collection;
    private final Class source;
    private final List<DBObject> stages = new ArrayList<DBObject>();
    private final Mapper mapper;
    private final dev.morphia.DatastoreImpl datastore;
    private boolean firstStage;

    /**
     * Creates an AggregationPipeline
     *
     * @param datastore  the datastore to use
     * @param collection the database collection on which to operate
     * @param source     the source type to aggregate
     */
    public AggregationPipelineImpl(final dev.morphia.DatastoreImpl datastore, final DBCollection collection, final Class source) {
        this.datastore = datastore;
        this.collection = collection;
        mapper = datastore.getMapper();
        this.source = source;
    }

    /**
     * @morphia.internal
     * @return the stages
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
    public <U> Iterator<U> aggregate(final Class<U> target, final AggregationOptions options,
                                     final ReadPreference readPreference) {
        return aggregate(datastore.getCollection(target).getName(), target, options, readPreference);
    }

    @Override
    public <U> Iterator<U> aggregate(final String collectionName, final Class<U> target,
                                     final AggregationOptions options,
                                     final ReadPreference readPreference) {
        LOG.debug("stages = " + stages);

        Cursor cursor = collection.aggregate(stages, options, readPreference);
        return new MorphiaCursor<U>(datastore, cursor, mapper, target, mapper.createEntityCache());
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
            group.putAll(toDBObject(grouping));
        }

        stages.add(new BasicDBObject("$group", group));
        return this;
    }

    @Override
    public AggregationPipeline group(final List<Group> id, final Group... groupings) {
        if (id != null) {
            DBObject idGroup = new BasicDBObject();
            for (Group group : id) {
                idGroup.putAll(toDBObject(group));
            }
            DBObject group = new BasicDBObject("_id", idGroup);
            for (Group grouping : groupings) {
                group.putAll(toDBObject(grouping));
            }
            stages.add(new BasicDBObject("$group", group));
        }

        return this;
    }

    @Override
    public AggregationPipeline limit(final int count) {
        stages.add(new BasicDBObject("$limit", count));
        return this;
    }

    @Override
    public AggregationPipeline lookup(final String from, final String localField,
                                      final String foreignField, final String as) {
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
    public AggregationPipeline sample(final int sampleSize) {
        stages.add(new BasicDBObject("$sample", new BasicDBObject("size", sampleSize)));
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
    public <U> Iterator<U> out(final String collectionName, final Class<U> target,
                               final AggregationOptions options) {
        stages.add(new BasicDBObject("$out", collectionName));
        return aggregate(target, options);
    }

    @Override
    public AggregationPipeline project(final Projection... projections) {
        firstStage = stages.isEmpty();
        DBObject dbObject = new BasicDBObject();
        for (Projection projection : projections) {
            dbObject.putAll(toDBObject(projection));
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
    public AggregationPipeline sort(final Sort... sorts) {
        DBObject sortList = new BasicDBObject();
        for (Sort sort : sorts) {
            sortList.put(sort.getField(), sort.getOrder());
        }

        stages.add(new BasicDBObject("$sort", sortList));
        return this;
    }

    @Override
    public AggregationPipeline unwind(final String field) {
        stages.add(new BasicDBObject("$unwind", "$" + field));
        return this;
    }

    @Override
    public AggregationPipeline unwind(final String field, final UnwindOptions options) {
        BasicDBObject unwindOptions = new BasicDBObject("path", "$" + field)
                .append("preserveNullAndEmptyArrays", options.isPreserveNullAndEmptyArrays());
        String includeArrayIndex = options.getIncludeArrayIndex();
        if (includeArrayIndex != null) {
            unwindOptions.append("includeArrayIndex", includeArrayIndex);
        }
        stages.add(new BasicDBObject("$unwind", unwindOptions));
        return this;
    }

    @Override
    public AggregationPipeline sortByCount(final String field) {
        stages.add(new BasicDBObject("$sortByCount", "$" + field));
        return this;
    }

    @Override
    public AggregationPipeline bucket(final String field, final List<?> boundaries) {
        return bucket(field, boundaries, new BucketOptions());
    }

    @Override
    public AggregationPipeline bucket(final String field, final List<?> boundaries, final BucketOptions options) {
        if (boundaries == null || boundaries.size() < 2) {
            throw new RuntimeException("Boundaries list should be present and has at least 2 elements");
        }
        DBObject dbObject = options.toDBObject();
        dbObject.put("groupBy", "$" + field);
        dbObject.put("boundaries", boundaries);
        stages.add(new BasicDBObject("$bucket", dbObject));
        return this;
    }

    @Override
    public AggregationPipeline bucketAuto(final String field, final int bucketCount) {
        return bucketAuto(field, bucketCount, new BucketAutoOptions());
    }

    @Override
    public AggregationPipeline bucketAuto(final String field, final int bucketCount, final BucketAutoOptions options) {

        if (bucketCount < 1) {
            throw new RuntimeException("bucket count should be more than 0");
        }
        DBObject dbObject = options.toDBObject();
        dbObject.put("groupBy", "$" + field);
        dbObject.put("buckets", bucketCount);
        stages.add(new BasicDBObject("$bucketAuto", dbObject));
        return this;
    }

    /**
     * Converts a Projection to a DBObject for use by the Java driver.
     *
     * @param projection the project to apply
     * @return the DBObject
     */
    private DBObject toDBObject(final Projection projection) {
        String target;
        if (firstStage) {
            MappedField field = mapper.getMappedClass(source).getMappedField(projection.getTarget());
            target = field != null ? field.getNameToStore() : projection.getTarget();
        } else {
            target = projection.getTarget();
        }

        if (projection.getProjections() != null) {
            List<Projection> list = projection.getProjections();
            DBObject projections = new BasicDBObject();
            for (Projection subProjection : list) {
                projections.putAll(toDBObject(subProjection));
            }
            return new BasicDBObject(target, projections);
        } else if (projection.getSource() != null) {
            return new BasicDBObject(target, projection.getSource());
        } else if (projection.getArguments() != null) {
            DBObject args = toExpressionArgs(projection.getArguments());
            if (target == null) {
                // Unwrap for single-argument expressions
                if (args instanceof List<?> && ((List<?>) args).size() == 1) {
                    Object firstArg = ((List<?>) args).get(0);
                    if (firstArg instanceof DBObject) {
                        return (DBObject) firstArg;
                    }
                }
                return args;
            } else {
                // Unwrap for single-argument expressions
                if (args instanceof List<?> && ((List<?>) args).size() == 1) {
                    return new BasicDBObject(target, ((List<?>) args).get(0));
                }
                return new BasicDBObject(target, args);
            }
        } else {
            return new BasicDBObject(target, projection.isSuppressed() ? 0 : 1);
        }
    }

    private DBObject toDBObject(final Group group) {
        BasicDBObject dbObject = new BasicDBObject();

        if (group.getAccumulator() != null) {
            dbObject.put(group.getName(), group.getAccumulator().toDBObject());
        } else if (group.getProjections() != null) {
            final BasicDBObject projection = new BasicDBObject();
            for (Projection p : group.getProjections()) {
                projection.putAll(toDBObject(p));
            }
            dbObject.put(group.getName(), projection);
        } else if (group.getNested() != null) {
            dbObject.put(group.getName(), toDBObject(group.getNested()));
        } else {
            dbObject.put(group.getName(), group.getSourceField());
        }

        return dbObject;
    }

    private void putIfNull(final DBObject dbObject, final String name, final Object value) {
        if (value != null) {
            dbObject.put(name, value);
        }
    }

    private DBObject toExpressionArgs(final List<Object> args) {
        BasicDBList result = new BasicDBList();
        for (Object arg : args) {
            if (arg instanceof Projection) {
                Projection projection = (Projection) arg;
                if (projection.getArguments() != null || projection.getProjections() != null
                    || projection.getSource() != null) {
                    result.add(toDBObject(projection));
                } else {
                    result.add("$" + projection.getTarget());
                }
            } else {
                result.add(arg);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return stages.toString();
    }
}
