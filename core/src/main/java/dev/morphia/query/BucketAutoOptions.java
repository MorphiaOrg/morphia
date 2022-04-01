package dev.morphia.query;

import dev.morphia.aggregation.Accumulator;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * The options for a bucket auto stage of aggregation pipeline.
 *
 * @author Roman Lapin
 * @deprecated use {@link dev.morphia.aggregation.stages.AutoBucket} instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public class BucketAutoOptions {

    private Granularity granularity;
    private final Map<String, Accumulator> accumulators = new HashMap<String, Accumulator>();


    /**
     * Converts a BucketAutoOptions to a Document for use by the Java driver.
     *
     * @return the Document
     */
    public Document toDocument() {
        Document document = new Document();
        if (granularity != null) {
            document.put("granularity", granularity.getGranulality());
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
     * Define granularity field for the bucketauto stage
     *
     * @param granularity granularity {@link Granularity}
     * @return this
     */
    public BucketAutoOptions granularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    /**
     * Define output field for the bucketauto stage
     *
     * @param fieldName name of the output field
     * @return this
     */
    public OutputOperation output(String fieldName) {

        return new OutputOperation(fieldName);
    }

    /**
     * Defines an output for bucketauto stage, that consists of the fieldname and
     * the accumulator
     */
    public class OutputOperation {

        private final String fieldName;

        /**
         * Creates the output operation for given fieldname
         *
         * @param fieldName name of the output field
         */
        public OutputOperation(String fieldName) {
            this.fieldName = fieldName;
        }

        /**
         * Returns an array of all unique values that results from applying an expression to each document
         * in a group of documents that share the same group by key. Order of the elements in the output array is unspecified.
         *
         * @param field the field to process
         * @return an Accumulator
         * @aggregation.expression $addToSet
         */
        public BucketAutoOptions addToSet(String field) {
            accumulators.put(fieldName, new Accumulator("$addToSet", field));
            return BucketAutoOptions.this;
        }

        /**
         * Returns the average value of the numeric values that result from applying a specified expression to each document in a group of
         * documents that share the same group by key. $avg ignores non-numeric values.
         *
         * @param field the field to process
         * @return an Accumulator
         * @aggregation.expression $avg
         */
        public BucketAutoOptions average(String field) {

            accumulators.put(fieldName, new Accumulator("$avg", field));
            return BucketAutoOptions.this;
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
        public BucketAutoOptions sum(Object field) {
            accumulators.put(fieldName, new Accumulator("$sum", field));
            return BucketAutoOptions.this;
        }


    }

    /**
     * @return granurality for the current bucketauto stage
     */
    public Granularity getGranularity() {
        return granularity;
    }

    /**
     * A value that specifies the preferred number series to use to ensure
     * that the calculated boundary edges end on preferred round numbers or their powers of 10.
     *
     * Available only if the all groupBy values are numeric and none of them are NaN.
     */
    public enum Granularity {
        R5("R5"),
        R10("R10"),
        R20("R20"),
        R40("R40"),
        R80("R80"),
        ONE_TWO_FIVE("1-2-5"),
        E6("E6"),
        E12("E12"),
        E24("E24"),
        E48("E48"),
        E96("E96"),
        E192("E192"),
        POWERSOF2("POWERSOF2");

        private final String granularity;

        Granularity(String granularity) {

            this.granularity = granularity;
        }

        /**
         * @return granurality string value
         */
        public String getGranulality() {
            return granularity;
        }
    }

    /**
     *
     * @return output accumulators per output field
     */
    public Map<String, Accumulator> getAccumulators() {
        return accumulators;
    }
}
