package dev.morphia.aggregation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.internal.DeferredDecoding;
import dev.morphia.sofia.Sofia;

import org.bson.Document;
import org.bson.RawBsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> the source type of the aggregation
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class AggregationImpl<T> implements Aggregation<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AggregationImpl.class);

    private final MorphiaDatastore datastore;

    private Class<?> targetType;

    @Nullable
    private final Class<T> source;

    private final AggregationOptions options;
    private final MongoCollection<T> collection;

    private final List<Stage> stages = new ArrayList<>();

    private MorphiaCursor<T> iterator;

    private String terminated;

    public AggregationImpl() {
        collection = null;
        source = null;
        options = null;
        datastore = null;
    }

    /**
     * @param datastore  the datastore to use
     * @param source     the source type, or null for collection-based aggregation
     * @param targetType the result type
     * @param options    the aggregation options
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @SuppressWarnings("unchecked")
    public AggregationImpl(MorphiaDatastore datastore, @Nullable Class<T> source, Class<T> targetType, AggregationOptions options) {
        this.datastore = datastore;
        this.source = source;
        this.options = options;
        this.targetType = targetType;
        this.collection = source != null && options.collection() == null
                ? datastore.getCollection(source)
                : (MongoCollection<T>) datastore.getDatabase().getCollection(options.collection());
    }

    @Override
    public Aggregation<T> pipeline(Stage... stages) {
        for (Stage stage : stages) {
            if (terminated != null) {
                throw new IllegalArgumentException(Sofia.aggregationTerminated(terminated));
            }
            addStage(stage);
            if (stage instanceof Merge || stage instanceof Out) {
                terminated = stage.stageName();
                iterator = iterator();
            } else if (stage instanceof Match match) {
                Filter[] filters = match.getFilters();
                Arrays.stream(filters)
                        .filter(f -> f.getName().equals("$eq"))
                        .forEach(f -> f.entityType(source));
            }
        }
        return this;
    }

    @Override
    public void close() throws Exception {
        if (iterator != null) {
            iterator.close();
            iterator = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public MorphiaCursor<T> iterator() {
        if (iterator == null) {
            List<Document> pipeline = pipeline();
            if (LOG.isDebugEnabled()) {
                LOG.debug("pipeline = " + pipeline);
            }
            if (DeferredDecoding.required(datastore)) {
                MongoCursor<RawBsonDocument> raw = (MongoCursor<RawBsonDocument>) (MongoCursor<?>) options
                        .apply(pipeline, datastore, collection, RawBsonDocument.class).iterator();
                iterator = new MorphiaCursor<>((MongoCursor<T>) DeferredDecoding.cursor(raw, collection.getCodecRegistry(), targetType));
            } else {
                iterator = new MorphiaCursor<>(options.apply(pipeline, datastore, collection, targetType).iterator());
            }
        }
        MorphiaCursor<T> cursor = iterator;
        iterator = null;
        return cursor;
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
