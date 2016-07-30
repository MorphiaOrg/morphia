package org.mongodb.morphia.aggregation;

import com.mongodb.DBObject;

/**
 * Defines a projection element in aggregation pipeline
 * @mongodb.driver.manual reference/operator/aggregation/project/ $project
 */
public interface ProjectionElement {
    /**
     * Converts a ProjectionElement to a DBObject for use by the Java driver.
     * @return the DBObject
     */
    DBObject toDBObject();
}
