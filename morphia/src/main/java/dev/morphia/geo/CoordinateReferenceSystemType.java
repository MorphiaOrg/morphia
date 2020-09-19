package dev.morphia.geo;

/**
 * An enumeration of the GeoJSON coordinate reference system types.
 * @deprecated use the driver-provided types instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public enum CoordinateReferenceSystemType {
    /**
     * A coordinate reference system that is specified by name
     */
    NAME("name"),

    /**
     * A coordinate reference system that is specified by a dereferenceable URI
     */
    LINK("link");

    private final String typeName;

    CoordinateReferenceSystemType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Gets the GeoJSON-defined name for the type.
     *
     * @return the GeoJSON-defined type name
     */
    public String getTypeName() {
        return typeName;
    }
}
