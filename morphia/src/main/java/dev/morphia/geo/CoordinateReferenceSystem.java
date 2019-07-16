package dev.morphia.geo;

/**
 * Defines the coordinate reference system to be used in certain geo queries.
 *
 * @mongodb.driver.manual reference/operator/query/geometry $geometry
 * @deprecated use the driver-provided types instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public abstract class CoordinateReferenceSystem {

    /**
     * Gets the type of this Coordinate Reference System.
     *
     * @return the type
     */
    public abstract CoordinateReferenceSystemType getType();

    /**
     * Converts this type to the driver type
     * @return the driver type
     */
    public abstract com.mongodb.client.model.geojson.CoordinateReferenceSystem convert();
}
