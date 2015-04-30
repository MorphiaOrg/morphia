package org.mongodb.morphia.geo;

import org.bson.util.annotations.Immutable;

import java.util.Map;

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
