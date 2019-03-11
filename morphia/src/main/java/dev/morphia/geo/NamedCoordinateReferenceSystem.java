/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.geo;

import static java.lang.String.format;

/**
 * A GeoJSON named Coordinate Reference System.
 */
public final class NamedCoordinateReferenceSystem extends CoordinateReferenceSystem {

    /**
     * The EPSG:4326 Coordinate Reference System.
     */
    public static final NamedCoordinateReferenceSystem EPSG_4326 =
        new NamedCoordinateReferenceSystem("EPSG:4326");

    /**
     * The urn:ogc:def:crs:OGC:1.3:CRS84 Coordinate Reference System
     */
    public static final NamedCoordinateReferenceSystem CRS_84 =
        new NamedCoordinateReferenceSystem("urn:ogc:def:crs:OGC:1.3:CRS84");

    /**
     * A custom MongoDB EPSG:4326 Coordinate Reference System that uses a strict counter-clockwise winding order.
     */
    public static final NamedCoordinateReferenceSystem EPSG_4326_STRICT_WINDING =
        new NamedCoordinateReferenceSystem("urn:x-mongodb:crs:strictwinding:EPSG:4326");

    private final String name;

    /**
     * Construct an instance
     *
     * @param name the name
     */
    private NamedCoordinateReferenceSystem(final String name) {
        this.name = name;

    }

    /**
     * Gets the name of this Coordinate Reference System.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public CoordinateReferenceSystemType getType() {
        return CoordinateReferenceSystemType.NAME;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NamedCoordinateReferenceSystem that = (NamedCoordinateReferenceSystem) o;

        return name.equals(that.name);

    }

    @Override
    public String toString() {
        return format("NamedCoordinateReferenceSystem{name='%s'}", name);
    }

    @Override
    public com.mongodb.client.model.geojson.CoordinateReferenceSystem convert() {
        return new com.mongodb.client.model.geojson.NamedCoordinateReferenceSystem(name);
    }
}
