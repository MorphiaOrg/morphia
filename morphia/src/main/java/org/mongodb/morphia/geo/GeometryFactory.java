package org.mongodb.morphia.geo;

import java.util.List;

interface GeometryFactory {
    Geometry createGeometry(List<?> geometries);
}
