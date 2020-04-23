package dev.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.aggregation.Accumulator;

import java.util.HashMap;
import java.util.Map;

/**
 * The options for a bucket stage of aggregation pipeline.
 *
 * @author Roman Lapin
 */
public class BucketOptions {

    private Object defaultField;
    private Map<String, Accumulator> accumulators = new HashMap<String, Accumulator>();


    /**
     * Converts a BucketOptions to a DBObject for use by the Java driver.
     *
     * @return the DBObject
     * @deprecated this is an internal method and is removed in 2.0
     */
    @Deprecated
    public DBObject toDBObject() {
        DBObject dbObject = new BasicDBObject();
        if (defaultField != null) {
            dbObject.put("default", defaultField);
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
         * @mongodb.driver.manual reference/operator/aggregation/addToSet $addToSet
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
         * @mongodb.driver.manual reference/operator/aggregation/avg $avg
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
         * @mongodb.driver.manual reference/operator/aggregation/sum $sum
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
