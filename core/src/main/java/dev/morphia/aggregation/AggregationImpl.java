package dev.morphia.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.ChangeStream;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.MorphiaCursor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> the starting type of the aggregation
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class AggregationImpl<T> implements Aggregation<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AggregationImpl.class);

    private final MorphiaDatastore datastore;
    private final Class<?> source;
    private final MongoCollection<T> collection;
    private final List<Stage> stages = new ArrayList<>();

    /**
     * Creates an instance.
     *
     * @param datastore  the datastore
     * @param collection the source collection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationImpl(MorphiaDatastore datastore, MongoCollection<T> collection) {
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationImpl(MorphiaDatastore datastore, Class<T> source, MongoCollection<T> collection) {
        this.datastore = datastore;
        this.source = source;
        this.collection = collection;
    }

    @Override
    public Aggregation<T> pipeline(Stage... stages) {
        for (Stage stage : stages) {
            addStage(stage);
        }
        return this;
    }

    @Override
    public void execute() {
        execute(Document.class);
    }

    @Override
    public void execute(AggregationOptions options) {
        execute(Document.class, options);
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
    public Aggregation<T> changeStream(ChangeStream stream) {
        return addStage(stream);
    }

    /**
     * @return the stages
     */
    public List<Stage> getStages() {
        return stages;
    }

    /**
     * @return the encoded pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Document> pipeline() {
        return stages.stream()
                .map(stage -> DocumentWriter.encode(stage, datastore.getMapper(), datastore.getCodecRegistry()))
                .collect(Collectors.toList());
    }

    private Aggregation<T> addStage(Stage stage) {
        stages.add(stage);
        return this;
    }
}
