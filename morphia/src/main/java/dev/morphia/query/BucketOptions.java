package dev.morphia.query;

import dev.morphia.aggregation.Accumulator;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * The options for a bucket stage of aggregation pipeline.
 *
 * @author Roman Lapin
 * @deprecated use {@link dev.morphia.aggregation.experimental.stages.Bucket} instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public class BucketOptions {

    private Object defaultField;
    private Map<String, Accumulator> accumulators = new HashMap<String, Accumulator>();


    /**
     * Converts a BucketOptions to a Document for use by the Java driver.
     *
     * @return the Document
     */
    public Document toDocument() {
        Document document = new Document();
        if (defaultField != null) {
            document.put("default", defaultField);
        }

        Document output = new Document();
        for (Map.Entry<String, Accumulator> entry : accumulators.entrySet()) {
            output.put(entry.getKey(), entry.getValue().toDocument());
        }
        if (!accumulators.isEmpty()) {
            document.put("output", output);
        }

        return document;
    }

    /**
     * Define default field for the bucket stage
     *
     * @param defaultField name of the field
     * @return this
     */
    public BucketOptions defaultField(final Object defaultField) {
        this.defaultField = defaultField;
        return this;
    }

    /**
     * Define output field for the bucket stage
     *
     * @param fieldName name of the output field
     * @return this
     */
    public OutputOperation output(final String fieldName) {

        return new OutputOperation(fieldName);
    }

    /**
     * Defines an output for bucketauto stage, that consists of the fieldname and
     * the accumulator
     */
    public class OutputOperation {

        private String fieldName;

        /**
         * Creates the output operation for given fieldname
         *
         * @param fieldName name of the output field
         */
        public OutputOperation(final String fieldName) {
            this.fieldName = fieldName;
        }

        /**
         * Returns an array of all unique values that results from applying
         * an expression to each document in a group of documents that share
         * the same group by key. Order of the elements in the output array is unspecified.
         *
         * @param field the field to process
         * @return an Accumulator
         * @aggregation.expression $addToSet
         */
        public BucketOptions addToSet(final String field) {
            accumulators.put(fieldName, new Accumulator("$addToSet", field));
            return BucketOptions.this;
        }

        /**
         * Returns the average value of the numeric values that result from applying a specified expression to each document in a group of
         * documents that share the same group by key. $avg ignores non-numeric values.
         *
         * @param field the field to process
         * @return an Accumulator
         * @aggregation.expression $avg
         */
        public BucketOptions average(final String field) {

            accumulators.put(fieldName, new Accumulator("$avg", field));
            return BucketOptions.this;
        }

        /**
         * Calculates and returns the sum of all the numeric values that result from applying a specified expression to each document in a
         * group
         * of documents that share the same group by key. $sum ignores non-numeric values.
         *
         * @param field the field to process
         * @return an Accumulator
         * @aggregation.expression $sum
         */
        public BucketOptions sum(final Object field) {
            accumulators.put(fieldName, new Accumulator("$sum", field));
            return BucketOptions.this;
        }


    }

    /**
     * @return default bucket name
     */
    public Object getDefaultField() {
        return defaultField;
    }

    /**
     * @return output accumulators per output field
     */
    public Map<String, Accumulator> getAccumulators() {
        return accumulators;
    }

}
