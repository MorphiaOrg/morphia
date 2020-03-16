package dev.morphia.aggregation.experimental;

import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Count;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.GeoNear;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.IndexStats;
import dev.morphia.aggregation.experimental.stages.Limit;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Out;
import dev.morphia.aggregation.experimental.stages.PlanCacheStats;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.ReplaceWith;
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
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @param <T>
 * @morphia.internal
 * @since 2.0
 */
public class AggregationImpl<T> implements Aggregation<T> {
    private final Datastore datastore;
    private final MongoCollection<T> collection;
    private final List<Stage> stages = new ArrayList<>();

    /**
     * Creates an instance.
     *
     * @param datastore  the datastore
     * @param collection the source collection
     * @morphia.internal
     */
    public AggregationImpl(final Datastore datastore, final MongoCollection<T> collection) {
        this.datastore = datastore;
        this.collection = collection;
    }

    @Override
    public Aggregation<T> addFields(final AddFields fields) {
        stages.add(fields);
        return this;
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
    public Aggregation<T> collStats(final CollectionStats stats) {
        stages.add(stats);
        return this;
    }

    @Override
    public Aggregation<T> count(final String name) {
        stages.add(new Count(name));
        return this;
    }

    @Override
    public Aggregation<T> currentOp(final CurrentOp currentOp) {
        stages.add(currentOp);
        return this;
    }

    @Override
    public <R> MorphiaCursor<R> execute(final Class<R> resultType) {
        return new MorphiaCursor<>(collection.aggregate(getDocuments(), resultType).iterator());
    }

    @Override
    public <R> MorphiaCursor<R> execute(final Class<R> resultType, final AggregationOptions options) {
        return new MorphiaCursor<>(options.apply(getDocuments(), collection, resultType)
                                          .iterator());
    }

    @Override
    public Aggregation<T> facet(final Facet facet) {
        stages.add(facet);
        return this;
    }

    @Override
    public Aggregation<T> geoNear(final GeoNear near) {
        stages.add(near);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Document> getDocuments() {
        return stages.stream()
                     .map(s -> {
                         Codec codec = datastore.getMapper().getCodecRegistry().get(s.getClass());
                         DocumentWriter writer = new DocumentWriter();
                         codec.encode(writer, s, EncoderContext.builder().build());
                         return writer.getDocument();
                     })
                     .collect(Collectors.toList());
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
    public Aggregation<T> graphLookup(final GraphLookup lookup) {
        stages.add(lookup);
        return this;
    }

    @Override
    public Aggregation<T> group(final Group group) {
        stages.add(group);
        return this;
    }

    @Override
    public Aggregation<T> indexStats() {
        stages.add(IndexStats.of());
        return this;
    }

    @Override
    public Aggregation<T> limit(final long limit) {
        stages.add(Limit.of(limit));
        return this;
    }

    @Override
    public Aggregation<T> lookup(final Lookup lookup) {
        stages.add(lookup);
        return this;
    }

    @Override
    public Aggregation<T> match(final Query<?> query) {
        stages.add(Match.on(query));
        return this;
    }

    @Override
    public <M> void merge(final Merge<M> merge) {
        stages.add(merge);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <M> void merge(final Merge<M> merge, final AggregationOptions options) {
        stages.add(merge);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <O> void out(final Out<O> out) {
        stages.add(out);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <O> void out(final Out<O> out, final AggregationOptions options) {
        stages.add(out);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public Aggregation<T> planCacheStats() {
        stages.add(PlanCacheStats.of());
        return this;
    }

    @Override
    public Aggregation<T> project(final Projection projection) {
        stages.add(projection);
        return this;
    }

    @Override
    public Aggregation<T> redact(final Redact redact) {
        stages.add(redact);
        return this;
    }

    @Override
    public Aggregation<T> replaceRoot(final ReplaceRoot root) {
        stages.add(root);
        return this;
    }

    @Override
    public Aggregation<T> replaceWith(final ReplaceWith with) {
        stages.add(with);
        return this;
    }

    @Override
    public Aggregation<T> sample(final long sample) {
        stages.add(Sample.of(sample));
        return this;
    }

    @Override
    public Aggregation<T> skip(final long skip) {
        stages.add(Skip.of(skip));
        return this;
    }

    @Override
    public Aggregation<T> sort(final Sort sort) {
        stages.add(sort);
        return this;
    }

    @Override
    public Aggregation<T> sortByCount(final Expression sort) {
        stages.add(SortByCount.on(sort));
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
