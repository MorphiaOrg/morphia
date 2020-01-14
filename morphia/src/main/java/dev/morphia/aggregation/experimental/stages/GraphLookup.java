package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.query.Query;

/**
 * Performs a recursive search on a collection, with options for restricting the search by recursion depth and query filter.
 *
 * @mongodb.driver.manual reference/operator/aggregation/graphLookup/ $graphLookup
 */
public class GraphLookup extends Stage {
    private String from;
    private Expression startWith;
    private String connectFromField;
    private String connectToField;
    private String as;
    private Integer maxDepth;
    private String depthField;
    private Query restriction;
    private Class fromType;

    /**
     * Creates a new stage using the target collection
     *
     * @param from the target collection
     */
    public GraphLookup(final String from) {
        this();
        this.from = from;
    }

    protected GraphLookup() {
        super("$graphLookup");
    }

    /**
     * Creates a new stage using the target collection for the mapped type
     *
     * @param from the type to use for determining the target collection
     */
    public GraphLookup(final Class from) {
        this();
        this.fromType = from;
    }

    /**
     * Target collection for the $graphLookup operation to search, recursively matching the connectFromField to the connectToField.
     *
     * @param from the target collection name
     * @return this
     */
    public static GraphLookup from(final String from) {
        return new GraphLookup(from);
    }

    /**
     * Target collection for the $graphLookup operation to search, recursively matching the connectFromField to the connectToField.
     *
     * @param from the target collection name
     * @return this
     */
    public static GraphLookup from(final Class from) {
        return new GraphLookup(from);
    }


    /**
     * Name of the array field added to each output document. Contains the documents traversed in the $graphLookup stage to reach the
     * document.
     *
     * @param as the name
     * @return this
     */
    public GraphLookup as(final String as) {
        this.as = as;
        return this;
    }

    /**
     * Field name whose value $graphLookup uses to recursively match against the connectToField of other documents in the collection. If
     * the value is an array, each element is individually followed through the traversal process.
     *
     * @param connectFromField the field name
     * @return this
     */
    public GraphLookup connectFromField(final String connectFromField) {
        this.connectFromField = connectFromField;
        return this;
    }

    /**
     * Field name in other documents against which to match the value of the field specified by the connectFromField parameter.
     *
     * @param connectToField the field name
     * @return this
     */
    public GraphLookup connectToField(final String connectToField) {
        this.connectToField = connectToField;
        return this;
    }

    /**
     * Optional. Name of the field to add to each traversed document in the search path. The value of this field is the recursion depth
     * for the document, represented as a NumberLong. Recursion depth value starts at zero, so the first lookup corresponds to zero depth.
     *
     * @param depthField the field name
     * @return this
     */
    public GraphLookup depthField(final String depthField) {
        this.depthField = depthField;
        return this;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getAs() {
        return as;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getConnectFromField() {
        return connectFromField;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getConnectToField() {
        return connectToField;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getDepthField() {
        return depthField;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getFrom() {
        return from;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Class getFromType() {
        return fromType;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Integer getMaxDepth() {
        return maxDepth;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Query getRestriction() {
        return restriction;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Expression getStartWith() {
        return startWith;
    }

    /**
     * Optional. Non-negative integral number specifying the maximum recursion depth.
     *
     * @param maxDepth the max depth
     * @return this
     */
    public GraphLookup maxDepth(final Integer maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    /**
     * Optional. A query specifying additional conditions for the recursive search
     *
     * @param query the query to restrict the matching
     * @return this
     */
    public GraphLookup restrict(final Query query) {
        this.restriction = query;
        return this;
    }

    /**
     * Expression that specifies the value of the connectFromField with which to start the recursive search.
     *
     * @param startWith the expression defining the starting point
     * @return this
     */
    public GraphLookup startWith(final Expression startWith) {
        this.startWith = startWith;
        return this;
    }
}
