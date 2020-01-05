package dev.morphia.aggregation.experimental;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.query.Query;

public class GraphLookup extends Stage {
    private String from;
    private Expression startWith;
    private String connectFromField;
    private String connectToField;
    private String as;
    private Integer maxDepth;
    private String depthField;
    private Query restrictWithMatch;

    protected GraphLookup() {
        super("$graphLookup");
    }

    public static GraphLookup with() {
        return new GraphLookup();
    }

    public GraphLookup as(final String as) {
        this.as = as;
        return this;
    }

    public GraphLookup connectFromField(final String connectFromField) {
        this.connectFromField = connectFromField;
        return this;
    }

    public GraphLookup connectToField(final String connectToField) {
        this.connectToField = connectToField;
        return this;
    }

    public GraphLookup depthField(final String depthField) {
        this.depthField = depthField;
        return this;
    }

    public GraphLookup from(final String from) {
        this.from = from;
        return this;
    }

    public String getAs() {
        return as;
    }

    public String getConnectFromField() {
        return connectFromField;
    }

    public String getConnectToField() {
        return connectToField;
    }

    public String getDepthField() {
        return depthField;
    }

    public String getFrom() {
        return from;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public Query getRestrictWithMatch() {
        return restrictWithMatch;
    }

    public Expression getStartWith() {
        return startWith;
    }

    public GraphLookup maxDepth(final Integer maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public GraphLookup restrictSearchWithMatch(final Query restrictSearchWithMatch) {
        this.restrictWithMatch = restrictSearchWithMatch;
        return this;
    }

    public GraphLookup startWith(final Expression startWith) {
        this.startWith = startWith;
        return this;
    }
}
