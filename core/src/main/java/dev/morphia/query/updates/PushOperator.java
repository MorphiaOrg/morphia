package dev.morphia.query.updates;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateException;
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
        if (position < 0) {
            throw new UpdateException("The position must be at least 0.");
        }
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

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);
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
