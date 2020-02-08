package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

import static dev.morphia.aggregation.experimental.expressions.Expressions.field;

/**
 * Deconstructs an array field from the input documents to output a document for each element. Each output document is the input document
 * with the value of the array field replaced by the element.
 *
 * @aggregation.expression $unwind
 */
public class Unwind extends Stage {
    private Expression path;
    private String includeArrayIndex;
    private Boolean preserveNullAndEmptyArrays;

    protected Unwind() {
        super("$unwind");
    }

    /**
     * Creates a stage with the named array field
     *
     * @param name the array field
     * @return this
     */
    public static Unwind on(final String name) {
        return new Unwind()
                   .path(name);
    }

    private Unwind path(final String path) {
        this.path = field(path);
        return this;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public String getIncludeArrayIndex() {
        return includeArrayIndex;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Expression getPath() {
        return path;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public Boolean getPreserveNullAndEmptyArrays() {
        return preserveNullAndEmptyArrays;
    }

    /**
     * Optional. The name of a new field to hold the array index of the element. The name cannot start with a dollar sign $.
     *
     * @param name the new name
     * @return this
     */
    public Unwind includeArrayIndex(final String name) {
        this.includeArrayIndex = name;
        return this;
    }

    /**
     * @return the value
     * @morphia.internal
     */
    public boolean optionsPresent() {
        return includeArrayIndex != null
               || preserveNullAndEmptyArrays != null;
    }

    /**
     * Optional.
     *
     * <li>If true, if the path is null, missing, or an empty array, $unwind outputs the document.
     * <li>If false, if path is null, missing, or an empty array, $unwind does not output a document.
     *
     * @param preserveNullAndEmptyArrays true to preserve
     * @return this
     */
    public Unwind preserveNullAndEmptyArrays(final Boolean preserveNullAndEmptyArrays) {
        this.preserveNullAndEmptyArrays = preserveNullAndEmptyArrays;
        return this;
    }
}
