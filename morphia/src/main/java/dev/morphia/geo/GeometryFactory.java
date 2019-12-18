package dev.morphia.geo;

import java.util.List;

@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
interface GeometryFactory {
    Geometry createGeometry(List<?> geometries);
}
