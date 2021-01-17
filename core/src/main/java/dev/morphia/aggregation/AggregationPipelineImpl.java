package dev.morphia.aggregation;

import com.mongodb.AggregationOptions;
import com.mongodb.ReadPreference;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UnwindOptions;
import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.BucketAutoOptions;
import dev.morphia.query.BucketOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of an AggregationPipeline.
 *
 * @deprecated
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public class AggregationPipelineImpl implements AggregationPipeline {
    private static final Logger LOG = LoggerFactory.getLogger(AggregationPipelineImpl.class);

    private final MongoCollection collection;
    private final Class source;
    private final List<Document> stages = new ArrayList<>();
    private final Mapper mapper;
    private final Datastore datastore;
    private boolean firstStage;

    /**
     * Creates an AggregationPipeline
     *
     * @param datastore  the datastore to use
     * @param collection the database collection on which to operate
     * @param source     the source type to aggregate
     */
    public AggregationPipelineImpl(Datastore datastore, MongoCollection collection, Class source) {
        this.datastore = datastore;
        this.collection = collection;
        mapper = datastore.getMapper();
        this.source = source;
    }

    @Override
    public <U> Iterator<U> aggregate(Class<U> target) {
        return aggregate(target, AggregationOptions.builder().build(), collection.getReadPreference());
    }

    @Override
    public <U> Iterator<U> aggregate(Class<U> target, AggregationOptions options) {
        return aggregate(target, options, collection.getReadPreference());
    }

    @Override
    public <U> Iterator<U> aggregate(Class<U> target, AggregationOptions options,
                                     ReadPreference readPreference) {
        return aggregate(mapper.getCollection(target).getNamespace().getCollectionName(), target, options, readPreference);
    }

    @Override
    public <U> Iterator<U> aggregate(String collectionName, Class<U> target,
                                     AggregationOptions options,
                                     ReadPreference readPreference) {
        LOG.debug("stages = " + stages);


        AggregateIterable<U> cursor = collection.aggregate(stages, target);
        return cursor.iterator();
    }

    @Override
    public AggregationPipeline bucket(String field, List<?> boundaries) {
        return bucket(field, boundaries, new BucketOptions());
    }

    @Override
    public AggregationPipeline bucket(String field, List<?> boundaries, BucketOptions options) {
        if (boundaries == null || boundaries.size() < 2) {
            throw new RuntimeException("Boundaries list should be present and has at least 2 elements");
        }
        Document document = options.toDocument();
        document.put("groupBy", "$" + field);
        document.put("boundaries", boundaries);
        stages.add(new Document("$bucket", document));
        return this;
    }

    @Override
    public AggregationPipeline bucketAuto(String field, int bucketCount) {
        return bucketAuto(field, bucketCount, new BucketAutoOptions());
    }

    @Override
    public AggregationPipeline bucketAuto(String field, int bucketCount, BucketAutoOptions options) {

        if (bucketCount < 1) {
            throw new RuntimeException("bucket count should be more than 0");
        }
        Document document = options.toDocument();
        document.put("groupBy", "$" + field);
        document.put("buckets", bucketCount);
        stages.add(new Document("$bucketAuto", document));
        return this;
    }

    @Override
    public AggregationPipeline geoNear(GeoNear geoNear) {
        throw new UnsupportedOperationException(Sofia.legacyOperation());
    }

    @Override
    public AggregationPipeline group(Group... groupings) {
        return group((String) null, groupings);
    }

    @Override
    public AggregationPipeline group(String id, Group... groupings) {
        Document group = new Document();
        group.put("_id", id != null ? "$" + id : null);
        for (Group grouping : groupings) {
            group.putAll(toDocument(grouping));
        }

        stages.add(new Document("$group", group));
        return this;
    }

    @Override
    public AggregationPipeline group(List<Group> id, Group... groupings) {
        if (id != null) {
            Document idGroup = new Document();
            for (Group group : id) {
                idGroup.putAll(toDocument(group));
            }
            Document group = new Document("_id", idGroup);
            for (Group grouping : groupings) {
                group.putAll(toDocument(grouping));
            }
            stages.add(new Document("$group", group));
        }

        return this;
    }

    @Override
    public AggregationPipeline limit(int count) {
        stages.add(new Document("$limit", count));
        return this;
    }

    @Override
    public AggregationPipeline lookup(String from, String localField,
                                      String foreignField, String as) {
        stages.add(new Document("$lookup", new Document("from", from)
                                               .append("localField", localField)
                                               .append("foreignField", foreignField)
                                               .append("as", as)));
        return this;
    }

    @Override
    public AggregationPipeline match(Query query) {
        stages.add(new Document("$match", query.disableValidation().toDocument()));
        return this;
    }

    @Override
    public <U> Iterator<U> out(Class<U> target) {
        return out(mapper.getCollection(target).getNamespace().getCollectionName(), target);
    }

    @Override
    public <U> Iterator<U> out(Class<U> target, AggregationOptions options) {
        return out(mapper.getCollection(target).getNamespace().getCollectionName(), target, options);
    }

    @Override
    public <U> Iterator<U> out(String collectionName, Class<U> target) {
        return out(collectionName, target, AggregationOptions.builder().build());
    }

    @Override
    public <U> Iterator<U> out(String collectionName, Class<U> target,
                               AggregationOptions options) {
        stages.add(new Document("$out", collectionName));
        return aggregate(target, options);
    }

    @Override
    public AggregationPipeline project(Projection... projections) {
        firstStage = stages.isEmpty();
        Document document = new Document();
        for (Projection projection : projections) {
            document.putAll(toDocument(projection));
        }
        stages.add(new Document("$project", document));
        return this;
    }

    @Override
    public AggregationPipeline sample(int sampleSize) {
        stages.add(new Document("$sample", new Document("size", sampleSize)));
        return this;
    }

    @Override
    public AggregationPipeline skip(int count) {
        stages.add(new Document("$skip", count));
        return this;
    }

    @Override
    public AggregationPipeline sort(Sort... sorts) {
        Document sortList = new Document();
        for (Sort sort : sorts) {
            sortList.put(sort.getField(), sort.getOrder());
        }

        stages.add(new Document("$sort", sortList));
        return this;
    }

    @Override
    public AggregationPipeline sortByCount(String field) {
        stages.add(new Document("$sortByCount", "$" + field));
        return this;
    }

    @Override
    public AggregationPipeline unwind(String field, UnwindOptions options) {
        Document unwindOptions = new Document("path", "$" + field)
                                     .append("preserveNullAndEmptyArrays", options.isPreserveNullAndEmptyArrays());
        String includeArrayIndex = options.getIncludeArrayIndex();
        if (includeArrayIndex != null) {
            unwindOptions.append("includeArrayIndex", includeArrayIndex);
        }
        stages.add(new Document("$unwind", unwindOptions));
        return this;
    }

    @Override
    public AggregationPipeline unwind(String field) {
        stages.add(new Document("$unwind", "$" + field));
        return this;
    }

    /**
     * @return the stages
     * @morphia.internal
     */
    public List<Document> getStages() {
        return stages;
    }

    @Override
    public String toString() {
        return stages.toString();
    }

    private void putIfNull(Document document, String name, Object value) {
        if (value != null) {
            document.put(name, value);
        }
    }

    private Document toDocument(Group group) {
        Document document = new Document();

        if (group.getAccumulator() != null) {
            document.put(group.getName(), group.getAccumulator().toDocument());
        } else if (group.getProjections() != null) {
            final Document projection = new Document();
            for (Projection p : group.getProjections()) {
                projection.putAll(toDocument(p));
            }
            document.put(group.getName(), projection);
        } else if (group.getNested() != null) {
            document.put(group.getName(), toDocument(group.getNested()));
        } else {
            document.put(group.getName(), group.getSourceField());
        }

        return document;
    }

    /**
     * Converts a Projection to a Document for use by the Java driver.
     *
     * @param projection the project to apply
     * @return the Document
     */
    private Document toDocument(Projection projection) {
        String target;
        if (firstStage) {
            PropertyModel property = mapper.getEntityModel(source).getProperty(projection.getTarget());
            target = property != null ? property.getMappedName() : projection.getTarget();
        } else {
            target = projection.getTarget();
        }

        if (projection.getProjections() != null) {
            List<Projection> list = projection.getProjections();
            Document projections = new Document();
            for (Projection subProjection : list) {
                projections.putAll(toDocument(subProjection));
            }
            return new Document(target, projections);
        } else if (projection.getSource() != null) {
            return new Document(target, projection.getSource());
        } else if (projection.getArguments() != null) {
            List<Object> args = toExpressionArgs(projection.getArguments());
            if (target == null) {
                // Unwrap for single-argument expressions
                if (args.size() == 1) {
                    Object firstArg = ((List<?>) args).get(0);
                    if (firstArg instanceof Document) {
                        return (Document) firstArg;
                    }
                }
                throw new UnsupportedOperationException("aggregation support pending");
                //                return args;
            } else {
                // Unwrap for single-argument expressions
                if (args.size() == 1) {
                    return new Document(target, ((List<?>) args).get(0));
                }
                return new Document(target, args);
            }
        } else {
            return new Document(target, projection.isSuppressed() ? 0 : 1);
        }
    }

    private List<Object> toExpressionArgs(List<Object> args) {
        List<Object> result = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof Projection) {
                Projection projection = (Projection) arg;
                if (projection.getArguments() != null || projection.getProjections() != null
                    || projection.getSource() != null) {
                    result.add(toDocument(projection));
                } else {
                    result.add("$" + projection.getTarget());
                }
            } else {
                result.add(arg);
            }
        }
        return result;
    }
}
