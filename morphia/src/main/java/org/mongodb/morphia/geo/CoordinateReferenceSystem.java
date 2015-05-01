package org.mongodb.morphia.geo;

/**
 * Defines the coordinate reference system to be used in certain geo queries.
 *
 * @see <a href="http://docs.mongodb.org/manual/reference/operator/query/geometry/#op._S_geometry">$geometry</a>
 */
public abstract class CoordinateReferenceSystem {

    /**
     * Gets the type of this Coordinate Reference System.
     *
     * @return the type
     */
    public abstract CoordinateReferenceSystemType getType();
}
