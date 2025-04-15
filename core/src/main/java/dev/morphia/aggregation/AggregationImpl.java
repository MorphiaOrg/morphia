package dev.morphia.aggregation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;

import dev.morphia.DatastoreImpl;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.AddFields;
import dev.morphia.aggregation.stages.AutoBucket;
import dev.morphia.aggregation.stages.Bucket;
import dev.morphia.aggregation.stages.ChangeStream;
import dev.morphia.aggregation.stages.CollectionStats;
import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.CurrentOp;
import dev.morphia.aggregation.stages.Densify;
import dev.morphia.aggregation.stages.Documents;
import dev.morphia.aggregation.stages.Facet;
import dev.morphia.aggregation.stages.Fill;
import dev.morphia.aggregation.stages.GeoNear;
import dev.morphia.aggregation.stages.GraphLookup;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.aggregation.stages.IndexStats;
import dev.morphia.aggregation.stages.Limit;
import dev.morphia.aggregation.stages.Lookup;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.PlanCacheStats;
import dev.morphia.aggregation.stages.Projection;
import dev.morphia.aggregation.stages.Redact;
import dev.morphia.aggregation.stages.ReplaceRoot;
import dev.morphia.aggregation.stages.ReplaceWith;
import dev.morphia.aggregation.stages.Sample;
import dev.morphia.aggregation.stages.Set;
import dev.morphia.aggregation.stages.SetWindowFields;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.Sort;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.aggregation.stages.Unset;
import dev.morphia.aggregation.stages.Unwind;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.internal.MorphiaCursor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T>
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
@SuppressWarnings("removal")
public class AggregationImpl<T> implements Aggregation<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AggregationImpl.class);

    private final DatastoreImpl datastore;
    private final Class<?> source;
    private final MongoCollection<T> collection;
    private final List<Stage> stages = new ArrayList<>();

    /**
     * Creates an instance.
     *
     * @param datastore  the datastore
     * @param collection the source collection
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationImpl(DatastoreImpl datastore, MongoCollection<T> collection) {
        this.datastore = datastore;
        this.collection = collection;
        this.source = null;
    }

    /**
     * Creates an instance.
     *
     * @param datastore  the datastore
     * @param source     the source type
     * @param collection the source collection
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationImpl(DatastoreImpl datastore, Class<T> source, MongoCollection<T> collection) {
        this.datastore = datastore;
        this.source = source;
        this.collection = collection;
    }

    @Override
    public Aggregation<T> autoBucket(AutoBucket bucket) {
        return addStage(bucket);
    }

    @Override
    public Aggregation<T> bucket(Bucket bucket) {
        return addStage(bucket);
    }

    @Override
    public Aggregation<T> collStats(CollectionStats stats) {
        return addStage(stats);
    }

    @Override
    public Aggregation<T> count(String name) {
        return addStage(new Count(name));
    }

    @Override
    public Aggregation<T> currentOp(CurrentOp currentOp) {
        return addStage(currentOp);
    }

    @Override
    public Aggregation<T> densify(Densify densify) {
        return addStage(densify);
    }

    @Override
    public Aggregation<T> documents(DocumentExpression... documents) {
        return addStage(Documents.documents(documents));
    }

    @Override
    public <R> MorphiaCursor<R> execute(Class<R> resultType) {
        List<Document> pipeline = pipeline();
        if (LOG.isDebugEnabled()) {
            LOG.debug("pipeline = " + pipeline);
        }
        return new MorphiaCursor<>(datastore.operations().<R> aggregate(this.collection, pipeline, resultType).iterator());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> MorphiaCursor<R> execute(Class<R> resultType, AggregationOptions options) {
        List<Document> pipeline = pipeline();
        if (LOG.isDebugEnabled()) {
            LOG.debug("pipeline = " + pipeline);
        }
        return new MorphiaCursor<>((MongoCursor<R>) options.apply(pipeline, datastore, collection, resultType).iterator());
    }

    @Override
    public Aggregation<T> facet(Facet facet) {
        return addStage(facet);
    }

    @Override
    public Aggregation<T> fill(Fill fill) {
        return addStage(fill);
    }

    @Override
    public Aggregation<T> geoNear(GeoNear near) {
        return addStage(near);
    }

    @Override
    public Aggregation<T> graphLookup(GraphLookup lookup) {
        return addStage(lookup);
    }

    @Override
    public Aggregation<T> group(Group group) {
        return addStage(group);
    }

    @Override
    public Aggregation<T> indexStats() {
        return addStage(IndexStats.indexStats());
    }

    @Override
    public Aggregation<T> limit(long limit) {
        return addStage(Limit.limit(limit));
    }

    @Override
    public Aggregation<T> lookup(Lookup lookup) {
        return addStage(lookup);
    }

    @Override
    public Aggregation<T> match(Filter... filters) {
        if (stages.isEmpty()) {
            Arrays.stream(filters)
                    .filter(f -> f.getName().equals("$eq"))
                    .forEach(f -> f.entityType(source));
        }
        return addStage(Match.match(filters));
    }

    @Override
    public <M> void merge(Merge<M> merge) {
        addStage(merge);
        datastore.operations().aggregate(collection, pipeline())
                .toCollection();
    }

    @Override
    public <M> void merge(Merge<M> merge, AggregationOptions options) {
        addStage(merge);
        Class<?> type = merge.getType();
        type = type != null ? type : Document.class;
        options.apply(pipeline(), datastore, collection, type)
                .toCollection();
    }

    @Override
    public <O> void out(Out<O> out) {
        addStage(out);
        datastore.operations()
                .aggregate(collection, pipeline())
                .toCollection();
    }

    @Override
    public <O> void out(Out<O> out, AggregationOptions options) {
        addStage(out);
        Class<?> type = out.type();
        type = type != null ? type : Document.class;
        options.apply(pipeline(), datastore, collection, type).toCollection();
    }

    @Override
    public Aggregation<T> planCacheStats() {
        return addStage(PlanCacheStats.planCacheStats());
    }

    @Override
    public Aggregation<T> project(Projection projection) {
        return addStage(projection);
    }

    @Override
    public Aggregation<T> redact(Redact redact) {
        return addStage(redact);
    }

    @Override
    public Aggregation<T> replaceRoot(ReplaceRoot root) {
        return addStage(root);
    }

    @Override
    public Aggregation<T> replaceWith(ReplaceWith with) {
        return addStage(with);
    }

    @Override
    public Aggregation<T> sample(long sample) {
        return addStage(Sample.sample(sample));
    }

    @Override
    public Aggregation<T> addFields(AddFields fields) {
        return addStage(fields);
    }

    @Override
    public Aggregation<T> set(Set set) {
        return addStage(set);
    }

    @Override
    public Aggregation<T> setWindowFields(SetWindowFields fields) {
        return addStage(fields);
    }

    @Override
    public Aggregation<T> skip(long skip) {
        return addStage(Skip.skip(skip));
    }

    @Override
    public Aggregation<T> sort(Sort sort) {
        return addStage(sort);
    }

    @Override
    public Aggregation<T> sortByCount(Expression sort) {
        return addStage(SortByCount.sortByCount(sort));
    }

    @Override
    public Aggregation<T> unionWith(Class<?> type, Stage first, Stage... others) {
        return addStage(new UnionWith(type, Expressions.toList(first, others)));
    }

    @Override
    public Aggregation<T> unionWith(String collection, Stage first, Stage... others) {
        return addStage(new UnionWith(collection, Expressions.toList(first, others)));
    }

    @Override
    public Aggregation<T> unset(Unset unset) {
        return addStage(unset);
    }

    @Override
    public Aggregation<T> unwind(Unwind unwind) {
        return addStage(unwind);
    }

    @Override
    public Aggregation<T> changeStream() {
        return addStage(ChangeStream.changeStream());
    }

    @Override
    public Aggregation<T> changeStream(ChangeStream stream) {
        return addStage(stream);
    }

    public List<Stage> getStages() {
        return stages;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Document> pipeline() {
        return stages.stream()
                .map(stage -> DocumentWriter.encode(stage, datastore.getMapper(), datastore.getCodecRegistry()))
                .collect(Collectors.toList());
    }

    @Override
    public Aggregation<T> addStage(Stage stage) {
        stage.aggregation(this);
        stages.add(stage);
        return this;
    }

    private static class MappingCursor<R> implements MongoCursor<R> {
        private final MongoCursor<Document> results;
        private final Codec<R> codec;
        private final String discriminator;

        MappingCursor(MongoCursor<Document> results, Codec<R> codec, String discriminator) {
            this.results = results;
            this.codec = codec;
            this.discriminator = discriminator;
        }

        @Override
        public void close() {
            results.close();
        }

        @Override
        public boolean hasNext() {
            return results.hasNext();
        }

        @Override
        public R next() {
            return map(results.next());
        }

        public int available() {
            return results.available();
        }

        @Override
        @Nullable
        public R tryNext() {
            return hasNext() ? next() : null;
        }

        @Override
        @Nullable
        public ServerCursor getServerCursor() {
            return results.getServerCursor();
        }

        @Override
        public ServerAddress getServerAddress() {
            return results.getServerAddress();
        }

        private R map(Document next) {
            next.remove(discriminator);
            return codec.decode(new DocumentReader(next), DecoderContext.builder().build());
        }
    }

}
