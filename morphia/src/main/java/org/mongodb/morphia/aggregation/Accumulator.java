/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
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

/**
 * Defines an accumulator for use in an aggregation pipeline.
 */
public class Accumulator {
    private final String operation;
    private final Object field;

    /**
     * Defines an accumulator for use in an aggregation pipeline.
     *
     * @param operation the accumulator operation
     * @param field     the field to use
     */
    public Accumulator(final String operation, final String field) {
        this(operation, (Object) ("$" + field));
    }

    /**
     * Defines an accumulator for use in an aggregation pipeline.
     *
     * @param operation the accumulator operation
     * @param field     the field to use
     */
    public Accumulator(final String operation, final Object field) {
        this.operation = operation;
        this.field = field;
    }

    /**
     * @return the field for this accumulator
     */
    public Object getField() {
        return field;
    }

    /**
     * @return the operation for this accumulator
     */
    public String getOperation() {
        return operation;
    }
}
