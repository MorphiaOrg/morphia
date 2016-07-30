package org.mongodb.morphia.aggregation;

import com.mongodb.DBObject;

/**
 * Marker interface defines group element in aggregation pipeline
 * @mongodb.driver.manual reference/operator/aggregation/group/ $group
 */
public interface GroupElement {
    /**
     * Converts a GroupElement to a DBObject for use by the Java driver.
     * @return the DBObject
     */
    DBObject toDBObject();
}
