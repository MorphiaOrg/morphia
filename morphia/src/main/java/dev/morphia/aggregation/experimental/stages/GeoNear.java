package dev.morphia.aggregation.experimental.stages;

import com.mongodb.client.model.geojson.Point;
import dev.morphia.query.Query;

public class GeoNear extends Stage {
    Point point;
    double[][] coordinates;
    String distanceField;
    Boolean spherical;
    Number maxDistance;
    Query query;
    Number distanceMultiplier;
    String includeLocs;
    Boolean uniqueDocs;
    Number minDistance;
    String key;

    protected GeoNear(final Point point) {
        this();
        this.point = point;
    }

    protected GeoNear() {
        super("$geoNear");
    }

    protected GeoNear(final double[][] coordinates) {
        this();
        this.coordinates = coordinates;
    }

    public static GeoNear to(final Point point) {
        return new GeoNear(point);
    }

    public static GeoNear to(final double[][] coordinates) {
        return new GeoNear(coordinates);
    }

    public GeoNear distanceField(final String distanceField) {
        this.distanceField = distanceField;
        return this;
    }

    public GeoNear distanceMultiplier(final Number distanceMultiplier) {
        this.distanceMultiplier = distanceMultiplier;
        return this;
    }

    public double[][] getCoordinates() {
        return coordinates;
    }

    public String getDistanceField() {
        return distanceField;
    }

    public Number getDistanceMultiplier() {
        return distanceMultiplier;
    }

    public String getIncludeLocs() {
        return includeLocs;
    }

    public String getKey() {
        return key;
    }

    public Number getMaxDistance() {
        return maxDistance;
    }

    public Number getMinDistance() {
        return minDistance;
    }

    public Point getPoint() {
        return point;
    }

    public Query getQuery() {
        return query;
    }

    public Boolean getSpherical() {
        return spherical;
    }

    public Boolean getUniqueDocs() {
        return uniqueDocs;
    }

    public GeoNear includeLocs(final String includeLocs) {
        this.includeLocs = includeLocs;
        return this;
    }

    public GeoNear key(final String key) {
        this.key = key;
        return this;
    }

    public GeoNear maxDistance(final Number maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    public GeoNear minDistance(final Number minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    public GeoNear query(final Query query) {
        this.query = query;
        return this;
    }

    public GeoNear spherical(final Boolean spherical) {
        this.spherical = spherical;
        return this;
    }

    public GeoNear uniqueDocs(final Boolean uniqueDocs) {
        this.uniqueDocs = uniqueDocs;
        return this;
    }
}
