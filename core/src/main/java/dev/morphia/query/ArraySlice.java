/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package dev.morphia.query;

import org.bson.Document;

import java.util.Arrays;

/**
 * Defines array slicing options for query projections.
 */
public class ArraySlice {
    private final Integer limit;
    private Integer skip;

    /**
     * Specifies the number of array elements to return
     *
     * @param limit the number of array elements to return
     * @query.filter $slice
     */
    public ArraySlice(int limit) {
        this.limit = limit;
    }

    /**
     * Specifies the number of array elements to skip.
     *
     * @param skip  the number of array elements to skip
     * @param limit the number of array elements to return
     * @query.filter $slice
     */
    public ArraySlice(int skip, int limit) {
        this.skip = skip;
        this.limit = limit;
    }

    /**
     * @return the limit to apply to the projection
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the skip value to apply to the projection
     */
    public Integer getSkip() {
        return skip;
    }

    Document toDatabase() {
        return new Document("$slice", skip == null ? limit : Arrays.asList(skip, limit));

    }
}
