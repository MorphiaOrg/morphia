package dev.morphia.geo;

import java.util.List;

interface GeometryFactory {
    Geometry createGeometry(List<?> geometries);
}
