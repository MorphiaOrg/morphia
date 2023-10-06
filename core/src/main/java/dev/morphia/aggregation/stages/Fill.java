package dev.morphia.aggregation.stages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;

import static java.util.Arrays.asList;

/**
 * Populates null and missing field values within documents.
 * <p>
 * You can use $fill to populate missing data points:
 * <ul>
 * <li>In a sequence based on surrounding values.
 * <li>With a fixed value.
 * </ul>
 *
 * @aggregation.expression $fill
 * @mongodb.server.release 5.3
 * @since 2.3
 */
public class Fill extends Stage {
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private Expression partitionBy;
    private List<String> partitionByFields;
    private Sort[] sortBy;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Fill() {
        super("$fill");
    }

    /**
     * Creates a new $fill stage
     *
     * @return the new stage
     */
    public static Fill fill() {
        return new Fill();
    }

    /**
     * Specifies an object indicating how to fill missing values in the target field.
     *
     * @param name  the field name
     * @param value the value to fill with
     * @return this
     */
    public Fill field(String name, ValueExpression value) {
        fields.put(name, value);
        return this;
    }

    /**
     * Specifies an object indicating how to fill missing values in the target field.
     *
     * @param name   the field name
     * @param method the method to use when filling in missing values
     * @return this
     */
    public Fill field(String name, Method method) {
        fields.put(name, method);
        return this;
    }

    /**
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Map<String, Object> fields() {
        return fields;
    }

    /**
     * Specifies an array of fields as the compound key to group the documents.
     *
     * @param partitionBy the partition expression
     * @return this
     */
    public Fill partitionBy(Expression partitionBy) {
        this.partitionBy = partitionBy;
        return this;
    }

    /**
     * @return this
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Expression partitionBy() {
        return partitionBy;
    }

    /**
     * Specifies an array of fields as the compound key to group the documents.
     *
     * @param fields the fields
     * @return this
     */
    public Fill partitionByFields(String... fields) {
        partitionByFields = asList(fields);
        return this;
    }

    /**
     * @return this
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public List<String> partitionByFields() {
        return partitionByFields;
    }

    /**
     * Specifies the field or fields to sort the documents within each partition.
     *
     * @param sorts the sorting values to apply
     * @return this
     */
    public Fill sortBy(Sort... sorts) {
        this.sortBy = sorts;
        return this;
    }

    /**
     * @return this
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Sort[] sortBy() {
        return sortBy;
    }

    /**
     * Possible methods for defining fill strategies.
     */
    public enum Method {
        /**
         * the linear method
         */
        LINEAR,
        /**
         * the locf method
         */
        LOCF
    }
}
