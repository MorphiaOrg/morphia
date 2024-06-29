package dev.morphia.query.updates;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;
import dev.morphia.sofia.Sofia;

import org.bson.Document;

/**
 * Defines the $push update operator
 *
 * @since 2.0
 */
public class PushOperator extends UpdateOperator {
    @Nullable
    private Integer position;
    @Nullable
    private Integer slice;
    @Nullable
    private Integer sort;
    @Nullable
    private Document sortDocument;

    /**
     * @param field  the field
     * @param values the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    PushOperator(String field, List<?> values) {
        super("$push", field, values);
    }

    @Nullable
    public Integer position() {
        return position;
    }

    /**
     * Sets the position for the update
     *
     * @param position the position in the array for the update
     * @return this
     */
    public PushOperator position(int position) {
        this.position = position;
        return this;
    }

    @Nullable
    public Integer slice() {
        return slice;
    }

    /**
     * Sets the slice value for the update
     *
     * @param slice the slice value for the update
     * @return this
     */
    public PushOperator slice(int slice) {
        this.slice = slice;
        return this;
    }

    @Nullable
    public Integer sort() {
        return sort;
    }

    /**
     * Sets the sort value for the update
     *
     * @param sort the sort value for the update
     * @return this
     */
    public PushOperator sort(int sort) {
        if (sortDocument != null) {
            throw new IllegalStateException(Sofia.updateSortOptions("Sort", "sort document"));
        }
        this.sort = sort;
        return this;
    }

    /**
     * Sets the sort value for the update
     *
     * @param value the sort criteria to add
     * @return this
     */
    public PushOperator sort(Sort value) {
        if (sort != null) {
            throw new IllegalStateException(Sofia.updateSortOptions("Sort document", "sort"));
        }
        if (sortDocument == null) {
            sortDocument = new Document();
        }
        sortDocument.put(value.getField(), value.getOrder());
        return this;
    }

    @Nullable
    public Document sortDocument() {
        return sortDocument;
    }

}
