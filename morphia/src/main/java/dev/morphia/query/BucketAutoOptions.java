package dev.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.aggregation.Accumulator;

import java.util.HashMap;
import java.util.Map;

/**
 * The options for a bucket auto stage of aggregation pipeline.
 *
 * @author Roman Lapin
 */
public class BucketAutoOptions {

    private Granularity granularity;
    private Map<String, Accumulator> accumulators = new HashMap<String, Accumulator>();


    /**
     * Converts a BucketAutoOptions to a DBObject for use by the Java driver.
     *
     * @return the DBObject
     */
    public DBObject toDBObject() {
        DBObject dbObject = new BasicDBObject();
        if (granularity != null) {
            dbObject.put("granularity", granularity.getGranulality());
        }

        DBObject output = new BasicDBObject();
        for (Map.Entry<String, Accumulator> entry : accumulators.entrySet()) {
            output.put(entry.getKey(), entry.getValue().toDBObject());
        }
        if (!accumulators.isEmpty()) {
            dbObject.put("output", output);
        }

        return dbObject;
    }

    /**
     * Define granularity field for the bucketauto stage
     *
     * @param granularity granularity {@link Granularity}
     * @return this
     */
    public BucketAutoOptions granularity(final Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    /**
     * Define output field for the bucketauto stage
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
         * Returns an array of all unique values that results from applying an expression to each document
         * in a group of documents that share the same group by key. Order of the elements in the output array is unspecified.
         *
         * @param field the field to process
         * @return an Accumulator
         * @mongodb.driver.manual reference/operator/aggregation/addToSet $addToSet
         */
        public BucketAutoOptions addToSet(final String field) {
            accumulators.put(fieldName, new Accumulator("$addToSet", field));
            return BucketAutoOptions.this;
        }

        /**
         * Returns the average value of the numeric values that result from applying a specified expression to each document in a group of
         * documents that share the same group by key. $avg ignores non-numeric values.
         *
         * @param field the field to process
         * @return an Accumulator
         * @mongodb.driver.manual reference/operator/aggregation/avg $avg
         */
        public BucketAutoOptions average(final String field) {

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
         * @mongodb.driver.manual reference/operator/aggregation/sum $sum
         */
        public BucketAutoOptions sum(final Object field) {
            accumulators.put(fieldName, new Accumulator("$sum", field));
            return BucketAutoOptions.this;
        }


    }

    /**
     *
     * @return granurality for the current bucketauto stage
     */
    public Granularity getGranurality() {
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

        private String granularity;

        Granularity(final String granularity) {

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
