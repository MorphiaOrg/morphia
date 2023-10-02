package dev.morphia.aggregation.stages;

import java.util.Collections;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Performs a union of two collections; i.e. $unionWith combines pipeline results from two collections into a single result set. The
 * stage outputs the combined result set (including duplicates) to the next stage.
 *
 * @aggregation.expression $unionWith
 * @since 2.1
 */
public class UnionWith extends Stage {
    private final List<Stage> stages;
    private Class<?> collectionType;
    private String collectionName;

    /**
     * Creates the new stage
     *
     * @param collection the collection to process
     * @param stages     the pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UnionWith(String collection, List<Stage> stages) {
        super("$unionWith");
        this.collectionName = collection;
        this.stages = Collections.unmodifiableList(stages);
    }

    /**
     * Creates the new stage
     *
     * @param type   the type to process
     * @param stages the pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UnionWith(Class<?> type, List<Stage> stages) {
        super("$unionWith");
        this.collectionType = type;
        this.stages = Collections.unmodifiableList(stages);
    }

    /**
     * @return the collection name
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @return the collection type
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Class<?> getCollectionType() {
        return collectionType;
    }

    /**
     * @return the pipeline
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<Stage> getStages() {
        return stages;
    }
}
