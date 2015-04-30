package org.mongodb.morphia.geo;

import java.util.Map;

/**
 * Defines the coordinate reference system to be used in certain geo queries.
 *
 * @see <a href="http://docs.mongodb.org/manual/reference/operator/query/geometry/#op._S_geometry">$geometry</a>
 */
public interface CRS {
    String getType();

    Map<String, String> getProperties();
}
