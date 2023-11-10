package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

import static dev.morphia.aggregation.expressions.Expressions.field;

/**
 * Deconstructs an array field from the input documents to output a document for each element. Each output document is the input document
 * with the value of the array field replaced by the element.
 *
 * @aggregation.stage $unwind
 */
public class Unwind extends Stage {
    private Expression path;
    private String includeArrayIndex;
    private Boolean preserveNullAndEmptyArrays;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Unwind() {
        super("$unwind");
    }

    /**
     * Creates a stage with the named array field
     *
     * @param name the array field
     * @return this
     * @since 2.2
     */
    public static Unwind unwind(String name) {
        return new Unwind()
                .path(name);
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String getIncludeArrayIndex() {
        return includeArrayIndex;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression getPath() {
        return path;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Boolean getPreserveNullAndEmptyArrays() {
        return preserveNullAndEmptyArrays;
    }

    /**
     * Optional. The name of a new field to hold the array index of the element. The name cannot start with a dollar sign $.
     *
     * @param name the new name
     * @return this
     */
    public Unwind includeArrayIndex(String name) {
        this.includeArrayIndex = name;
        return this;
    }

    /**
     * @return the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean optionsPresent() {
        return includeArrayIndex != null
                || preserveNullAndEmptyArrays != null;
    }

    /**
     * Optional.
     *
     * <ol>
     * <li>If true, if the path is null, missing, or an empty array, $unwind outputs the document.
     * <li>If false, if path is null, missing, or an empty array, $unwind does not output a document.
     * </ol>
     *
     * @param preserveNullAndEmptyArrays true to preserve
     * @return this
     */
    public Unwind preserveNullAndEmptyArrays(Boolean preserveNullAndEmptyArrays) {
        this.preserveNullAndEmptyArrays = preserveNullAndEmptyArrays;
        return this;
    }

    private Unwind path(String path) {
        this.path = field(path);
        return this;
    }
}
