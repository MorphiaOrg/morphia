package dev.morphia.aggregation.experimental;

import com.mongodb.MongoCommandException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Count;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.query.Query;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AggregationImpl<T> implements Aggregation<T> {
    private final Datastore datastore;
    private final MongoCollection<T> collection;
    private final List<Stage> stages = new ArrayList<>();

    public AggregationImpl(final Datastore datastore, final MongoCollection<T> collection) {
        this.datastore = datastore;
        this.collection = collection;
    }

    @Override
    public Aggregation<T> autoBucket(final AutoBucket bucket) {
        stages.add(bucket);
        return this;
    }

    @Override
    public Aggregation<T> bucket(final Bucket bucket) {
        stages.add(bucket);
        return this;
    }

    @Override
    public Aggregation<T> graphLookup(final GraphLookup lookup) {
        stages.add(lookup);
        return this;
    }

    @Override
    public Aggregation<T> collStats(final CollectionStats stats) {
        stages.add(stats);
        return this;
    }

    @Override
    public Aggregation<T> currentOp(final CurrentOp currentOp) {
        stages.add(currentOp);
        return this;
    }

    @Override
    public AggregationImpl count(final String name) {
        stages.add(new Count(name));
        return this;
    }

    @Override
    public <S> MorphiaCursor<S> execute(final Class<S> resultType) {
        return new MorphiaCursor<>(collection.aggregate(getDocuments(), resultType).iterator());
    }

    @Override
    public <S> MorphiaCursor<S> execute(final Class<S> resultType, final AggregationOptions options) {
        AggregateIterable<S> aggregate = options.apply(collection).aggregate(getDocuments(), resultType)
                                                .allowDiskUse(options.allowDiskUse())
                                                .batchSize(options.batchSize())
                                                .bypassDocumentValidation(options.bypassDocumentValidation())
                                                .collation(options.collation())
                                                .maxTime(options.getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        return new MorphiaCursor<>(aggregate.iterator());
    }

    @Override
    public AggregationImpl facet(final Facet facet) {
        stages.add(facet);
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Document> getDocuments() {
        List<Document> collect = stages.stream()
                                       .map(s -> {
                                           Codec codec = datastore.getMapper().getCodecRegistry().get(s.getClass());
                                           DocumentWriter writer = new DocumentWriter();
                                           codec.encode(writer, s, EncoderContext.builder().build());
                                           return writer.<Document>getRoot();
                                       })
                                       .collect(Collectors.toList());
        return collect;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Stage> S getStage(final String name) {
        List<Stage> list = stages.stream()
                                 .filter(s -> s.getStageName().equals(name))
                                 .collect(Collectors.toList());
        return ((S) (list.size() == 1
                     ? list.get(0)
                     : list));
    }

    @Override
    public List<Stage> getStages() {
        return stages;
    }

    @Override
    public Aggregation<T> group(final Group group) {
        stages.add(group);
        return this;
    }

    @Override
    public Aggregation<T> limit(final int limit) {
        stages.add(Limit.of(limit));
        return this;
    }

    @Override
    public Aggregation<T> lookup(final Lookup lookup) {
        stages.add(lookup);
        return this;
    }

    @Override
    public AggregationImpl match(final Query query) {
        stages.add(Match.of(query));
        return this;
    }

    @Override
    public <O> void out(final Class<O> resultType) {
        out(datastore.getMapper().getMappedClass(resultType).getCollectionName());
    }

    @Override
    public void out(final String collectionName) {
        List<Document> documents = getDocuments();
        documents.add(new Document("$out", collectionName));
        collection.aggregate(documents)
                  .toCollection();
    }

    @Override
    public void out(final String collectionName, final AggregationOptions options) {
        List<Document> documents = getDocuments();
        documents.add(new Document("$out", collectionName));
        try {
            options.apply(collection)
                   .aggregate(documents)
                   .allowDiskUse(options.allowDiskUse())
                   .batchSize(options.batchSize())
                   .bypassDocumentValidation(options.bypassDocumentValidation())
                   .collation(options.collation())
                   .maxTime(options.getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                   .toCollection();
        } catch (MongoCommandException e) {
            throw new AggregationException(Sofia.aggregationFailed(documents), e);
        }
    }

    @Override
    public <O> void out(final Class<O> resultType, final AggregationOptions options) {
        out(datastore.getMapper().getMappedClass(resultType).getCollectionName(), options);
    }

    @Override
    public Aggregation<T> project(final Projection projection) {
        stages.add(projection);
        return this;
    }

    @Override
    public Aggregation<T> sample(final Sample sample) {
        stages.add(sample);
        return this;
    }

    @Override
    public Aggregation<T> addFields(final AddFields fields) {
        stages.add(fields);
        return this;
    }

    @Override
    public Aggregation<T> skip(final Skip skip) {
        stages.add(skip);
        return this;
    }

    @Override
    public Aggregation<T> sort(final Sort sort) {
        stages.add(sort);
        return this;
    }

    @Override
    public Aggregation<T> sortByCount(final SortByCount sort) {
        stages.add(sort);
        return this;
    }

    @Override
    public Aggregation<T> unset(final Unset unset) {
        stages.add(unset);
        return this;
    }

    @Override
    public Aggregation<T> unwind(final Unwind unwind) {
        stages.add(unwind);
        return this;
    }
}
