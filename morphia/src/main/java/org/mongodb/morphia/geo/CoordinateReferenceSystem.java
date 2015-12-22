package org.mongodb.morphia.geo;

/**
 * Defines the coordinate reference system to be used in certain geo queries.
 *
 * @mongodb.driver.manual reference/operator/query/geometry $geometry
 */
public abstract class CoordinateReferenceSystem {

    /**
     * Gets the type of this Coordinate Reference System.
     *
     * @return the type
     */
    public abstract CoordinateReferenceSystemType getType();
}
