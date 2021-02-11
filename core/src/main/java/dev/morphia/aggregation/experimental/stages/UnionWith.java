package dev.morphia.aggregation.experimental.stages;

import com.mongodb.lang.Nullable;

import java.util.List;

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
     * @morphia.internal
     */
    public UnionWith(String collection, List<Stage> stages) {
        super("$unionWith");
        this.collectionName = collection;
        this.stages = stages;
    }

    /**
     * Creates the new stage
     *
     * @param type   the type to process
     * @param stages the pipeline
     * @morphia.internal
     */
    public UnionWith(Class<?> type, List<Stage> stages) {
        super("$unionWith");
        this.collectionType = type;
        this.stages = stages;
    }

    /**
     * @return the collection name
     * @morphia.internal
     */
    @Nullable
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @return the collection type
     * @morphia.internal
     */
    public Class<?> getCollectionType() {
        return collectionType;
    }

    /**
     * @return the pipeline
     * @morphia.internal
     */
    public List<Stage> getStages() {
        return stages;
    }
}
