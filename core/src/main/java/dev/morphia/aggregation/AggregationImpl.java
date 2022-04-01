package dev.morphia.aggregation;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.AddFields;
import dev.morphia.aggregation.stages.AutoBucket;
import dev.morphia.aggregation.stages.Bucket;
import dev.morphia.aggregation.stages.CollectionStats;
import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.CurrentOp;
import dev.morphia.aggregation.stages.Facet;
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
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.internal.MorphiaCursor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @param <T>
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class AggregationImpl<T> implements Aggregation<T> {
    private final Datastore datastore;
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
    public AggregationImpl(Datastore datastore, MongoCollection<T> collection) {
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
    public AggregationImpl(Datastore datastore, Class<T> source, MongoCollection<T> collection) {
        this.datastore = datastore;
        this.source = source;
        this.collection = collection;
    }

    @Override
    public Aggregation<T> addFields(AddFields fields) {
        addStage(fields);
        return this;
    }

    @Override
    public Aggregation<T> autoBucket(AutoBucket bucket) {
        addStage(bucket);
        return this;
    }

    @Override
    public Aggregation<T> bucket(Bucket bucket) {
        addStage(bucket);
        return this;
    }

    @Override
    public Aggregation<T> collStats(CollectionStats stats) {
        addStage(stats);
        return this;
    }

    @Override
    public Aggregation<T> count(String name) {
        addStage(new Count(name));
        return this;
    }

    @Override
    public Aggregation<T> currentOp(CurrentOp currentOp) {
        addStage(currentOp);
        return this;
    }

    @Override
    public Aggregation<T> facet(Facet facet) {
        addStage(facet);
        return this;
    }

    @Override
    public <R> MorphiaCursor<R> execute(Class<R> resultType) {
        MongoCursor<R> cursor;
        if (datastore.getMapper().isMappable(resultType) && !resultType.equals(this.collection.getDocumentClass())) {
            MongoCollection<Document> collection = this.collection.withDocumentClass(Document.class);
            MongoCursor<Document> results = collection.aggregate(getDocuments()).iterator();
            EntityModel entityModel = datastore.getMapper().getEntityModel(this.collection.getDocumentClass());
            cursor = new MappingCursor<>(results, datastore.getCodecRegistry().get(resultType),
                entityModel.getDiscriminatorKey());
        } else {
            cursor = collection.aggregate(getDocuments(), resultType).iterator();
        }
        return new MorphiaCursor<>(cursor);
    }

    @Override
    public <R> MorphiaCursor<R> execute(Class<R> resultType, AggregationOptions options) {
        return new MorphiaCursor<>(options.apply(getDocuments(), collection, resultType)
                                          .iterator());
    }

    @Override
    public Aggregation<T> geoNear(GeoNear near) {
        addStage(near);
        return this;
    }

    @Override
    public Aggregation<T> graphLookup(GraphLookup lookup) {
        addStage(lookup);
        return this;
    }

    @Override
    public Aggregation<T> group(Group group) {
        addStage(group);
        return this;
    }

    @Override
    public Aggregation<T> indexStats() {
        addStage(IndexStats.indexStats());
        return this;
    }

    @Override
    public Aggregation<T> limit(long limit) {
        addStage(Limit.limit(limit));
        return this;
    }

    @Override
    public Aggregation<T> lookup(Lookup lookup) {
        addStage(lookup);
        return this;
    }

    @Override
    public Aggregation<T> match(Filter... filters) {
        if (stages.isEmpty()) {
            Arrays.stream(filters)
                  .filter(f -> f.getName().equals("$eq"))
                  .forEach(f -> f.entityType(source));
        }
        addStage(Match.match(filters));
        return this;
    }

    @Override
    public <M> void merge(Merge<M> merge) {
        addStage(merge);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <M> void merge(Merge<M> merge, AggregationOptions options) {
        addStage(merge);
        Class<?> type = merge.getType();
        type = type != null ? type : Document.class;
        options.apply(getDocuments(), collection, type)
               .toCollection();
    }

    @Override
    public <O> void out(Out<O> out) {
        addStage(out);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <O> void out(Out<O> out, AggregationOptions options) {
        addStage(out);
        Class<?> type = out.getType();
        type = type != null ? type : Document.class;
        options.apply(getDocuments(), collection, type).toCollection();
    }

    @Override
    public Aggregation<T> planCacheStats() {
        addStage(PlanCacheStats.planCacheStats());
        return this;
    }

    @Override
    public Aggregation<T> project(Projection projection) {
        addStage(projection);
        return this;
    }

    @Override
    public Aggregation<T> redact(Redact redact) {
        addStage(redact);
        return this;
    }

    @Override
    public Aggregation<T> replaceRoot(ReplaceRoot root) {
        addStage(root);
        return this;
    }

    @Override
    public Aggregation<T> replaceWith(ReplaceWith with) {
        addStage(with);
        return this;
    }

    @Override
    public Aggregation<T> sample(long sample) {
        addStage(Sample.sample(sample));
        return this;
    }

    @Override
    public Aggregation<T> set(Set set) {
        addStage(set);
        return this;
    }

    @Override
    public Aggregation<T> setWindowFields(SetWindowFields fields) {
        addStage(fields);
        return this;
    }

    @Override
    public Aggregation<T> skip(long skip) {
        addStage(Skip.skip(skip));
        return this;
    }

    @Override
    public Aggregation<T> sort(Sort sort) {
        addStage(sort);
        return this;
    }

    @Override
    public Aggregation<T> sortByCount(Expression sort) {
        addStage(SortByCount.sortByCount(sort));
        return this;
    }

    @Override
    public Aggregation<T> unionWith(Class<?> type, Stage first, Stage... others) {
        addStage(new UnionWith(type, Expressions.toList(first, others)));
        return this;
    }

    @Override
    public Aggregation<T> unionWith(String collection, Stage first, Stage... others) {
        addStage(new UnionWith(collection, Expressions.toList(first, others)));
        return this;
    }

    @Override
    public Aggregation<T> unset(Unset unset) {
        addStage(unset);
        return this;
    }

    @Override
    public Aggregation<T> unwind(Unwind unwind) {
        addStage(unwind);
        return this;
    }

    private void addStage(Stage stage) {
        stage.aggregation(this);
        stages.add(stage);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Document> getDocuments() {
        return stages.stream()
                     .map(s -> {
                         Codec codec = datastore.getCodecRegistry().get(s.getClass());
                         DocumentWriter writer = new DocumentWriter(datastore.getMapper());
                         codec.encode(writer, s, EncoderContext.builder().build());
                         return writer.getDocument();
                     })
                     .collect(Collectors.toList());
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
