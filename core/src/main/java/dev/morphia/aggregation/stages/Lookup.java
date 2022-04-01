package dev.morphia.aggregation.stages;

import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Performs a left outer join to an unsharded collection in the same database to filter in documents from the “joined” collection for
 * processing. To each input document, the $lookup stage adds a new array field whose elements are the matching documents from the
 * “joined” collection. The $lookup stage passes these reshaped documents to the next stage.
 *
 * @aggregation.expression $lookup
 * @since 2.0
 */
public class Lookup extends Stage {
    private String from;
    private Class<?> fromType;
    private String localField;
    private String foreignField;
    private String as;
    private DocumentExpression variables;
    private List<Stage> pipeline;

    protected Lookup(Class<?> fromType) {
        super("$lookup");
        this.fromType = fromType;
    }

    protected Lookup(String from) {
        super("$lookup");
        this.from = from;
    }

    /**
     * Creates a new stage using the target collection for the mapped type
     *
     * @param from the type to use for determining the target collection
     * @return the new stage
     * @deprecated use {@link #lookup(Class)}
     */
    @Deprecated(forRemoval = true)
    public static Lookup from(Class<?> from) {
        return new Lookup(from);
    }

    /**
     * Creates a new stage using the target collection
     *
     * @param from the target collection
     * @return the new stage
     * @deprecated use {@link #lookup(String)}
     */
    @Deprecated(forRemoval = true)
    public static Lookup from(String from) {
        return new Lookup(from);
    }

    /**
     * Creates a new stage using the target collection for the mapped type
     *
     * @param from the type to use for determining the target collection
     * @return the new stage
     * @since 2.2
     */
    public static Lookup lookup(Class<?> from) {
        return new Lookup(from);
    }

    /**
     * Creates a new stage using the target collection
     *
     * @param from the target collection
     * @return the new stage
     * @since 2.2
     */
    public static Lookup lookup(String from) {
        return new Lookup(from);
    }

    /**
     * Name of the array field added to each output document. Contains the documents traversed in the $graphLookup stage to reach the
     * document.
     *
     * @param as the name
     * @return this
     */
    public Lookup as(String as) {
        this.as = as;
        return this;
    }

    /**
     * Specifies the field from the documents in the from collection.
     *
     * @param foreignField the field name
     * @return this
     */
    public Lookup foreignField(String foreignField) {
        this.foreignField = foreignField;
        return this;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public String getAs() {
        return as;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public String getForeignField() {
        return foreignField;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public String getFrom() {
        return from;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public Class<?> getFromType() {
        return fromType;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    @Nullable
    public String getLocalField() {
        return localField;
    }

    /**
     * @return the embeded pipeline
     * @morphia.internal
     */
    @Nullable
    public List<Stage> getPipeline() {
        return pipeline;
    }

    /**
     * @return the embeded pipeline's variables
     * @morphia.internal
     */
    public DocumentExpression getVariables() {
        return variables;
    }

    /**
     * Defines a variable
     *
     * @param name       the variable name
     * @param expression the variable value expression
     * @return this
     */
    public Lookup let(String name, Expression expression) {
        if (variables == null) {
            variables = Expressions.of();
        }
        variables.field(name, expression);
        return this;
    }

    /**
     * Specifies the field from the documents input to the $lookup stage.
     *
     * @param localField the field name
     * @return this
     */
    public Lookup localField(String localField) {
        this.localField = localField;
        return this;
    }

    /**
     * Specifies the pipeline to run on the joined collection. The pipeline determines the resulting documents from the joined collection.
     * To return all documents, specify an empty pipeline.
     * <p>
     * The pipeline cannot include the $out stage or the $merge stage.
     *
     * @param stages the stages of the embedded pipeline
     * @return this
     * @since 2.2
     */
    public Lookup pipeline(Stage... stages) {
        pipeline = asList(stages);
        return this;
    }
}
