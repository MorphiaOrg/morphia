package dev.morphia.aggregation.stages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

import static java.util.Arrays.asList;

/**
 * Writes the results of the aggregation pipeline to a specified collection. The $merge operator must be the last stage in the pipeline.
 *
 * @param <M> the entity type
 * @aggregation.stage $merge
 */
public class Merge<M> extends Stage {
    private Class<M> type;

    private String database;

    private String collection;

    private List<String> on;

    private Map<String, Expression> variables;

    private WhenMatched whenMatched;

    private List<Stage> whenMatchedPipeline;

    private WhenNotMatched whenNotMatched;

    /**
     * @param type the target merge type
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Merge(Class<M> type) {
        this();
        this.type = type;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Merge() {
        super("$merge");
    }

    /**
     * @param collection the target collection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Merge(String collection) {
        this();
        this.collection = collection;
    }

    /**
     * @param database   the target database
     * @param collection the target collection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Merge(String database, String collection) {
        this();
        this.database = database;
        this.collection = collection;
    }

    /**
     * Creates a new stage targeting the collection mapped for the given type
     *
     * @param type the target type
     * @param <M>  the entity type
     * @return the new stage
     */
    public static <M> Merge<M> merge(Class<M> type) {
        return new Merge<>(type);
    }

    /**
     * Creates a new stage targeting the collection
     *
     * @param collection the target collection
     * @return the new stage
     */
    public static Merge<?> merge(String collection) {
        return new Merge<>(collection);
    }

    /**
     * Creates a new stage targeting the database and collection
     *
     * @param database   the target database
     * @param collection the target collection
     * @return the new stage
     */
    public static Merge<?> merge(String database, String collection) {
        return new Merge<>(database, collection);
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public String collection() {
        return collection;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public String database() {
        return database;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public List<String> on() {
        return on;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Class<M> type() {
        return type;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Map<String, Expression> variables() {
        return variables;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public WhenMatched getWhenMatched() {
        return whenMatched;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public List<Stage> whenMatchedPipeline() {
        return whenMatchedPipeline;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public WhenNotMatched whenNotMatched() {
        return whenNotMatched;
    }

    /**
     * Specifies a variable accessible for use in the whenMatched pipeline
     *
     * @param variable the variable name
     * @param value    the value expression
     * @return this
     */
    public Merge<M> let(String variable, Expression value) {
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        variables.put(variable, value);
        return this;
    }

    /**
     * Optional. Field or fields that act as a unique identifier for a document. The identifier determines if a results document matches
     * an already existing document in the output collection.
     *
     * @param field  the first field
     * @param fields the other fields
     * @return this
     */
    public Merge<M> on(String field, String... fields) {
        List<String> list = new ArrayList<>();
        list.add(field);
        list.addAll(asList(fields));
        this.on = list;
        return this;
    }

    /**
     * Optional. The behavior of $merge if a result document and an existing document in the collection have the same value for the
     * specified on field(s).
     *
     * @param whenMatched the behavior
     * @return this
     */
    public Merge<M> whenMatched(WhenMatched whenMatched) {
        this.whenMatched = whenMatched;
        return this;
    }

    /**
     * Optional. An aggregation pipeline to update the document in the collection.
     *
     * @param pipeline the pipeline
     * @return this
     */
    public Merge<M> whenMatched(List<Stage> pipeline) {
        this.whenMatchedPipeline = Collections.unmodifiableList(pipeline);
        return this;
    }

    /**
     * Optional. An aggregation pipeline to update the document in the collection.
     *
     * @param pipeline the pipeline
     * @return this
     */
    public Merge<M> whenMatched(Stage... pipeline) {
        this.whenMatchedPipeline = asList(pipeline);
        return this;
    }

    /**
     * Optional. The behavior of $merge if a result document does not match an existing document in the out collection.
     *
     * @param whenNotMatched the behavior
     * @return this
     */
    public Merge<M> whenNotMatched(WhenNotMatched whenNotMatched) {
        this.whenNotMatched = whenNotMatched;
        return this;
    }
}
