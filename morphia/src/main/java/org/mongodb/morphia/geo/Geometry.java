package org.mongodb.morphia.geo;

import java.util.List;

/**
 * Interface to denote which entities are classes that will serialise into a MongoDB GeoJson object.
 */
public interface Geometry {
    /**
     * Returns a list of coordinates for this Geometry type.  For something like a Point, this will be a pair of lat/long coordinates, but
     * for more complex types this will be a list of other Geometry objects.  Used for serialisation to MongoDB.
     *
     * @return a List containing either Geometry objects, or a pair of coordinates as doubles
     */
    List<?> getCoordinates();
}
