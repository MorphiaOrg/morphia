package org.mongodb.morphia.aggregation;

import com.mongodb.DBObject;

/**
 * Defines a sort element in an aggregation pipeline
 * @mongodb.driver.manual reference/operator/aggregation/sort/ $sort
 */
public interface SortElement {

    /**
     * Converts a SortElement to a DBObject for use by the Java driver.
     * @return DBObject representation of sort element
     */
    DBObject toDBObject();
}
