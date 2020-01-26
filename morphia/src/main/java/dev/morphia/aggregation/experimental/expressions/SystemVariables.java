package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

import static dev.morphia.aggregation.experimental.expressions.Expressions.value;

/**
 * Defines helper fields for referencing system variables
 */
public final class SystemVariables {
    /**
     * A variable that returns the current timestamp value.
     * CLUSTER_TIME is only available on replica sets and sharded clusters.
     * <p>
     * CLUSTER_TIME returns the same value for all members of the deployment and remains the same throughout all stages of
     * the pipeline.
     */
    public static final Expression CLUSTER_TIME = value("$$CLUSTER_TIME");
    /**
     * References the start of the field path being processed in the aggregation pipeline stage. Unless documented otherwise, all
     * stages start with CURRENT the same as ROOT.
     * <p>
     * CURRENT is modifiable. However, since $<field> is equivalent to $$CURRENT.<field>, rebinding CURRENT changes the
     * meaning of $ accesses.
     */
    public static final Expression CURRENT = value("$$CURRENT");
    /**
     * One of the allowed results of a $redact expression.
     */
    public static final Expression DESCEND = value("$$DESCEND");
    /**
     * One of the allowed results of a $redact expression.
     */
    public static final Expression KEEP = value("$$KEEP");
    /**
     * A variable that returns the current datetime value. NOW returns the same value for all members of the deployment and remains
     * the same throughout all stages of the aggregation pipeline.
     */
    public static final Expression NOW = value("$$NOW");
    /**
     * One of the allowed results of a $redact expression.
     */
    public static final Expression PRUNE = value("$$PRUNE");
    /**
     * A variable which evaluates to the missing value. Allows for the conditional exclusion of fields. In a $projection, a field
     * set to the variable REMOVE is excluded from the output.
     * <p>
     * For an example of its usage, see Conditionally Exclude Fields.
     */
    public static final Expression REMOVE = value("$$REMOVE");
    /**
     * References the root document, i.e. the top-level document, currently being processed in the aggregation pipeline stage.
     */
    public static final Expression ROOT = value("$$ROOT");

    private SystemVariables() {
    }
}
