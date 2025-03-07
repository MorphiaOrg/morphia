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

import java.util.Arrays;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

/**
 * Defines array slicing options for query projections.
 * 
 * @query.filter $slice
 */
public class ArraySlice {
    private final Integer limit;
    private Integer skip;

    /**
     * Specifies the number of array elements to return
     * 
     * @hidden
     *
     * @param limit the number of array elements to return
     */
    public ArraySlice(int limit) {
        this.limit = limit;
    }

    /**
     * @param limit the limit to impose
     * @return this
     * @query.filter $slice
     */
    public static ArraySlice limit(int limit) {
        return new ArraySlice(limit);
    }

    /**
     * @param skip the amount to skip
     * @return this
     */
    public ArraySlice skip(Integer skip) {
        this.skip = skip;
        return this;
    }

    /**
     * @return the limit to apply to the projection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the skip value to apply to the projection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Integer getSkip() {
        return skip;
    }

    Document toDatabase() {
        return new Document("$slice", skip == null ? limit : Arrays.asList(skip, limit));

    }
}
