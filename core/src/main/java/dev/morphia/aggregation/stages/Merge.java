package dev.morphia.aggregation.stages;

import com.mongodb.client.model.MergeOptions.WhenMatched;
import com.mongodb.client.model.MergeOptions.WhenNotMatched;
import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.expressions.impls.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Writes the results of the aggregation pipeline to a specified collection. The $merge operator must be the last stage in the pipeline.
 *
 * @param <M> the entity type
 * @aggregation.expression $merge
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

    protected Merge(Class<M> type) {
        this();
        this.type = type;
    }

    protected Merge() {
        super("$merge");
    }

    protected Merge(String collection) {
        this();
        this.collection = collection;
    }

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
    public static <M> Merge<M> into(Class<M> type) {
        return new Merge<>(type);
    }

    /**
     * Creates a new stage targeting the collection
     *
     * @param collection the target collection
     * @return the new stage
     */
    public static Merge<?> into(String collection) {
        return new Merge<>(collection);
    }

    /**
     * Creates a new stage targeting the database and collection
     *
     * @param database   the target database
     * @param collection the target collection
     * @return the new stage
     */
    public static Merge<?> into(String database, String collection) {
        return new Merge<>(database, collection);
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public String getDatabase() {
        return database;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public List<String> getOn() {
        return on;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public Class<M> getType() {
        return type;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Map<String, Expression> getVariables() {
        return variables;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public WhenMatched getWhenMatched() {
        return whenMatched;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public List<Stage> getWhenMatchedPipeline() {
        return whenMatchedPipeline;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public WhenNotMatched getWhenNotMatched() {
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
