package dev.morphia.aggregation.stages;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.lang.Nullable;
import dev.morphia.query.filters.Filter;

/**
 * Outputs documents in order of nearest to farthest from a specified point.
 *
 * @aggregation.expression $geoNear
 */
public class GeoNear extends Stage {
    private Point point;
    private double[] coordinates;
    private String distanceField;
    private Boolean spherical;
    private Number maxDistance;
    private Filter[] filters;
    private Number distanceMultiplier;
    private String includeLocs;
    private Number minDistance;
    private String key;

    protected GeoNear(Point point) {
        this();
        this.point = point;
    }

    protected GeoNear() {
        super("$geoNear");
    }

    protected GeoNear(double[] coordinates) {
        this();
        this.coordinates = coordinates;
    }

    /**
     * Creates a new geoNear stage
     *
     * @param point the center point
     * @return the new stage
     * @since 2.2
     */
    public static GeoNear geoNear(Point point) {
        return new GeoNear(point);
    }

    /**
     * Creates a new geoNear stage
     *
     * @param coordinates the center point coordinates
     * @return the new stage
     * @since 2.2
     */
    public static GeoNear geoNear(double[] coordinates) {
        return new GeoNear(coordinates);
    }

    /**
     * Creates a new geoNear stage
     *
     * @param point the center point
     * @return the new stage
     * @deprecated user {@link #geoNear(Point)}
     */
    @Deprecated(forRemoval = true)
    public static GeoNear to(Point point) {
        return new GeoNear(point);
    }

    /**
     * Creates a new geoNear stage
     *
     * @param coordinates the center point coordinates
     * @return the new stage
     * @deprecated use {@link #geoNear(double[])}
     */
    @Deprecated(forRemoval = true)
    public static GeoNear to(double[] coordinates) {
        return new GeoNear(coordinates);
    }

    /**
     * The output field that contains the calculated distance.
     *
     * @param distanceField the field name
     * @return this
     */
    public GeoNear distanceField(String distanceField) {
        this.distanceField = distanceField;
        return this;
    }

    /**
     * Optional. The factor to multiply all distances returned by the query.  For example, use the distanceMultiplier to convert radians,
     * as returned by a spherical query, to kilometers by multiplying by the radius of the Earth.
     *
     * @param distanceMultiplier the multiplier
     * @return this
     */
    public GeoNear distanceMultiplier(Number distanceMultiplier) {
        this.distanceMultiplier = distanceMultiplier;
        return this;
    }

    /**
     * @return the coordinates
     * @morphia.internal
     */
    public double[] getCoordinates() {
        return coordinates;
    }

    /**
     * @return the distance field
     * @morphia.internal
     */
    public String getDistanceField() {
        return distanceField;
    }

    /**
     * @return the distance multiplier
     * @morphia.internal
     */
    public Number getDistanceMultiplier() {
        return distanceMultiplier;
    }

    /**
     * @return the query
     * @morphia.internal
     */
    @Nullable
    public Filter[] getFilters() {
        return filters;
    }

    /**
     * @return includeLocs
     * @morphia.internal
     */
    public String getIncludeLocs() {
        return includeLocs;
    }

    /**
     * @return the key
     * @morphia.internal
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the max distance
     * @morphia.internal
     */
    public Number getMaxDistance() {
        return maxDistance;
    }

    /**
     * @return the min distance
     * @morphia.internal
     */
    public Number getMinDistance() {
        return minDistance;
    }

    /**
     * @return the point
     * @morphia.internal
     */
    public Point getPoint() {
        return point;
    }

    /**
     * @return spherical?
     * @morphia.internal
     */
    public Boolean getSpherical() {
        return spherical;
    }

    /**
     * Optional. This specifies the output field that identifies the location used to calculate the distance. This option is useful when
     * a location field contains multiple locations.
     *
     * @param includeLocs include the field if true
     * @return this
     */
    public GeoNear includeLocs(String includeLocs) {
        this.includeLocs = includeLocs;
        return this;
    }

    /**
     * Optional. Specify the geospatial indexed field to use when calculating the distance.
     *
     * @param key the indexed field
     * @return this
     */
    public GeoNear key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Optional. The maximum distance from the center point that the documents can be. MongoDB limits the results to those documents that
     * fall within the specified distance from the center point.
     * <p>
     * Specify the distance in meters if the specified point is GeoJSON and in radians if the specified point is legacy coordinate pairs.
     *
     * @param maxDistance the max distance
     * @return this
     */
    public GeoNear maxDistance(Number maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /**
     * Optional. The minimum distance from the center point that the documents can be. MongoDB limits the results to those documents that
     * fall outside the specified distance from the center point.
     * <p>
     * Specify the distance in meters for GeoJSON data and in radians for legacy coordinate pairs.
     *
     * @param minDistance the min distance
     * @return this
     */
    public GeoNear minDistance(Number minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    /**
     * Optional. Limits the results to the documents that match the query.
     * <p>
     * You cannot specify a $near predicate in the query field of the $geoNear stage.
     *
     * @param filters the filters to apply
     * @return this
     */
    public GeoNear query(Filter... filters) {
        this.filters = filters;
        return this;
    }

    /**
     * Optional. Determines how MongoDB calculates the distance between two points:
     *
     * <li>When true, MongoDB uses $nearSphere semantics and calculates distances using spherical geometry.
     * <li>When false, MongoDB uses $near semantics: spherical geometry for 2dsphere indexes and planar geometry for 2d indexes.
     *
     * @param spherical true if spherical
     * @return this
     */
    public GeoNear spherical(Boolean spherical) {
        this.spherical = spherical;
        return this;
    }
}
