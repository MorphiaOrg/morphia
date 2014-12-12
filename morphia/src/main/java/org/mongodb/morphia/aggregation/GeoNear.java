/*
 * Copyright (c) 2008 - 2013 MongoDB, Inc. <http://mongodb.com>
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

package org.mongodb.morphia.aggregation;

import org.mongodb.morphia.query.Query;

public final class GeoNear {
    private final double[] near;
    private final String distanceField;
    private final Long limit;
    private final Long maxDocuments;
    private final Double maxDistance;
    private final Query query;
    private final Boolean spherical;
    private final Double distanceMultiplier;
    private final String includeLocations;
    private final Boolean uniqueDocuments;

    private GeoNear(final GeoNearBuilder builder) {
        near = builder.near;
        distanceField = builder.distanceField;
        limit = builder.limit;
        maxDocuments = builder.maxDocuments;
        maxDistance = builder.maxDistance;
        query = builder.query;
        spherical = builder.spherical;
        distanceMultiplier = builder.distanceMultiplier;
        includeLocations = builder.includeLocations;
        uniqueDocuments = builder.uniqueDocuments;
    }

    public double[] getNear() {
        double[] copy = new double[0];
        if (near != null) {
            copy = new double[near.length];
            System.arraycopy(near, 0, copy, 0, near.length);
        }
        return copy;
    }

    public String getDistanceField() {
        return distanceField;
    }

    public Long getLimit() {
        return limit;
    }

    public Long getMaxDocuments() {
        return maxDocuments;
    }

    public Double getMaxDistance() {
        return maxDistance;
    }

    public Query getQuery() {
        return query;
    }

    public Boolean getSpherical() {
        return spherical;
    }

    public Double getDistanceMultiplier() {
        return distanceMultiplier;
    }

    public String getIncludeLocations() {
        return includeLocations;
    }

    public Boolean getUniqueDocuments() {
        return uniqueDocuments;
    }

    public static GeoNearBuilder builder(final String distanceField) {
        return new GeoNearBuilder(distanceField);
    }

    public static class GeoNearBuilder {
        private final String distanceField;
        private Long limit;
        private Long maxDocuments;
        private Double maxDistance;
        private Query query;
        private Boolean spherical;
        private Double distanceMultiplier;
        private String includeLocations;
        private Boolean uniqueDocuments;
        private double[] near;

        /**
         * @param distanceField The output field that contains the calculated distance. To specify a field within a subdocument, use dot
         *                      notation.
         * @see <a href="http://docs.mongodb.org/master/reference/glossary/#term-dot-notation">dot notation</a>
         */
        public GeoNearBuilder(final String distanceField) {
            this.distanceField = distanceField;
        }

        public GeoNearBuilder setNear(final double latitude, final double longitude) {
            this.near = new double[]{latitude, longitude};
            return this;
        }

        /**
         * The maximum number of documents to return. The default value is 100.
         *
         * @see #setMaxDocuments(Long).
         */
        public GeoNearBuilder setLimit(final Long limit) {
            this.limit = limit;
            return this;
        }

        /**
         * The maxDocuments option provides the same function as the limit option. Both define the maximum number of documents to return. If
         * both options are included, this value overrides the limit value.
         */
        public GeoNearBuilder setMaxDocuments(final Long num) {
            this.maxDocuments = num;
            return this;
        }

        /**
         * A distance from the center point. Specify the distance in radians. MongoDB limits the results to those documents that fall within
         * the specified distance from the center point.
         */
        public GeoNearBuilder setMaxDistance(final Double maxDistance) {
            this.maxDistance = maxDistance;
            return this;
        }

        /**
         * Limits the results to the documents that match the query. The query syntax is the usual MongoDB read operation query syntax.
         *
         * @see <a href="http://docs.mongodb.org/manual/tutorial/query-documents/#read-operations-query-argument">MongoDB read operation
         * query syntax</a>
         */
        public GeoNearBuilder setQuery(final Query query) {
            this.query = query;
            return this;
        }

        /**
         * If true, MongoDB references points using a spherical surface. The default value is false.
         */
        public GeoNearBuilder setSpherical(final Boolean spherical) {
            this.spherical = spherical;
            return this;
        }

        /**
         * The factor to multiply all distances returned by the query. For example, use the distanceMultiplier to convert radians, as
         * returned by a spherical query, to kilometers by multiplying by the radius of the Earth.
         */
        public GeoNearBuilder setDistanceMultiplier(final Double distanceMultiplier) {
            this.distanceMultiplier = distanceMultiplier;
            return this;
        }

        /**
         * This specifies the output field that identifies the location used to calculate the distance. This option is useful when a
         * location field contains multiple locations. To specify a field within a subdocument, use dot notation.
         *
         * @see <a href="http://docs.mongodb.org/master/reference/glossary/#term-dot-notation">dot notation</a>
         */
        public GeoNearBuilder setIncludeLocations(final String includeLocations) {
            this.includeLocations = includeLocations;
            return this;
        }

        /**
         * If this value is true, the query returns a matching document once, even if more than one of the document's location fields match
         * the query. If this value is false, the query returns a document multiple times if the document has multiple matching location
         * fields. See $uniqueDocs for more information.
         *
         * @param uniqueDocuments true if only unique documents are required in the return value
         * @return this builder
         * @see <a href="http://docs.mongodb.org/master/reference/operator/query/uniqueDocs/#op._S_uniqueDocs">uniqueDocs</a>
         * @deprecated Deprecated since server version 2.6: Geospatial queries no longer return duplicate results. The $uniqueDocs operator
         * has no impact on results.
         */
        @Deprecated
        public GeoNearBuilder setUniqueDocuments(final Boolean uniqueDocuments) {
            this.uniqueDocuments = uniqueDocuments;
            return this;
        }

        public GeoNear build() {
            return new GeoNear(this);
        }
    }
}