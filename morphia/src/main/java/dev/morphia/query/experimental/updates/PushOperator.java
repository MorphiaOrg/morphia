package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateException;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.List;

/**
 * Defines the $push update operator
 *
 * @morphia.internal
 * @since 2.0
 */
public class PushOperator extends UpdateOperator {
    private Integer position;
    private Integer slice;
    private Integer sort;
    private Document sortDocument;

    /**
     * @param field  the field
     * @param values the values
     * @morphia.internal
     */
    PushOperator(final String field, final List<?> values) {
        super("$push", field, values);
    }

    /**
     * Sets the position for the update
     *
     * @param position the position in the array for the update
     * @return this
     */
    public PushOperator position(final int position) {
        if (position < 0) {
            throw new UpdateException("The position must be at least 0.");
        }
        this.position = position;
        return this;
    }

    /**
     * Sets the slice value for the update
     *
     * @param slice the slice value for the update
     * @return this
     */
    public PushOperator slice(final int slice) {
        this.slice = slice;
        return this;
    }

    /**
     * Sets the sort value for the update
     *
     * @param sort the sort value for the update
     * @return this
     */
    public PushOperator sort(final int sort) {
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
    public PushOperator sort(final Sort value) {
        if (sort != null) {
            throw new IllegalStateException(Sofia.updateSortOptions("Sort document", "sort"));
        }
        if (sortDocument == null) {
            sortDocument = new Document();
        }
        sortDocument.put(value.getField(), value.getOrder());
        return this;
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        Document document = new Document("$each", value());
        if (position != null) {
            document.put("$position", position);
        }
        if (slice != null) {
            document.put("$slice", slice);
        }
        if (sort != null) {
            document.put("$sort", sort);
        }
        if (sortDocument != null) {
            document.put("$sort", sortDocument);
        }

        return new OperationTarget(pathTarget, document);
    }
}
