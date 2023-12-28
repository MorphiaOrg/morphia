package dev.morphia.aggregation.stages;

import java.util.Collections;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Performs a union of two collections; i.e. $unionWith combines pipeline results from two collections into a single result set. The
 * stage outputs the combined result set (including duplicates) to the next stage.
 *
 * @aggregation.stage $unionWith
 * @since 2.1
 */
public class UnionWith extends Stage {
    private final List<Stage> pipeline;
    private Class<?> collectionType;
    private String collectionName;

    /**
     * Creates the new stage
     *
     * @param collection the collection to process
     * @param pipeline   the pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    private UnionWith(String collection, List<Stage> pipeline) {
        super("$unionWith");
        this.collectionName = collection;
        this.pipeline = Collections.unmodifiableList(pipeline);
    }

    /**
     * Creates the new stage
     *
     * @param type     the type to process
     * @param pipeline the pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    private UnionWith(Class<?> type, List<Stage> pipeline) {
        super("$unionWith");
        this.collectionType = type;
        this.pipeline = Collections.unmodifiableList(pipeline);
    }

    /**
     * Performs a union of two collections; i.e. $unionWith combines pipeline results from two collections into a single result set. The
     * stage outputs the combined result set (including duplicates) to the next stage.
     *
     * @param type   the type to perform the pipeline against
     * @param stages the pipeline stages
     * 
     * @return the new Stage
     *
     * @aggregation.stage $unionWith
     * @mongodb.server.release 4.4
     */
    public static Stage unionWith(Class<?> type, Stage... stages) {
        return new UnionWith(type, Expressions.toList(stages));
    }

    /**
     * Performs a union of two collections; i.e. $unionWith combines pipeline results from two collections into a single result set. The
     * stage outputs the combined result set (including duplicates) to the next stage.
     *
     * @param collection the collection to perform the pipeline against
     * @param stages     the pipeline stages
     *
     * @return the new stage
     *
     * @aggregation.stage $unionWith
     * @mongodb.server.release 4.4
     */
    static public Stage unionWith(String collection, Stage... stages) {
        return new UnionWith(collection, Expressions.toList(stages));
    }

    /**
     * @return the collection name
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public String collectionName() {
        return collectionName;
    }

    /**
     * @return the collection type
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Class<?> collectionType() {
        return collectionType;
    }

    /**
     * @return the pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<Stage> pipeline() {
        return pipeline;
    }
}
