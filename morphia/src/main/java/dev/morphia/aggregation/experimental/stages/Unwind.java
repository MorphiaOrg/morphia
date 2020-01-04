package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;

import static dev.morphia.aggregation.experimental.expressions.Expression.field;

public class Unwind extends Stage {
    private Expression path;
    private String includeArrayIndex;
    private Boolean preserveNullAndEmptyArrays;

    protected Unwind() {
        super("$unwind");
    }

    public static Unwind on(final String path) {
        return new Unwind()
            .path(path);
    }

    public String getIncludeArrayIndex() {
        return includeArrayIndex;
    }

    public Expression getPath() {
        return path;
    }

    public Boolean getPreserveNullAndEmptyArrays() {
        return preserveNullAndEmptyArrays;
    }

    public Unwind includeArrayIndex(final String includeArrayIndex) {
        this.includeArrayIndex = includeArrayIndex;
        return this;
    }

    public boolean optionsPresent() {
        return includeArrayIndex != null
            || preserveNullAndEmptyArrays != null;
    }

    public Unwind path(final String path) {
        this.path = field(path);
        return this;
    }

    public Unwind preserveNullAndEmptyArrays(final Boolean preserveNullAndEmptyArrays) {
        this.preserveNullAndEmptyArrays = preserveNullAndEmptyArrays;
        return this;
    }
}
