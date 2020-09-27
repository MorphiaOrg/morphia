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

package dev.morphia.aggregation;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an accumulator for use in an aggregation pipeline.
 */
public class Accumulator implements AggregationElement {
    private final String operation;
    private final Object value;

    /**
     * Defines an accumulator for use in an aggregation pipeline.
     *
     * @param operation the accumulator operation
     * @param field     the field to use
     */
    public Accumulator(String operation, String field) {
        this(operation, (Object) ("$" + field));
    }

    /**
     * Defines an accumulator for use in an aggregation pipeline.
     *
     * @param operation the accumulator operation
     * @param field     the field to use
     */
    public Accumulator(String operation, Object field) {
        this.operation = operation;
        this.value = field;
    }

    /**
     * Defines an accumulator for use in an aggregation pipeline.
     *
     * @param operation the accumulator operation
     * @param field     the field to use
     * @return an Accumulator
     */
    public static Accumulator accumulator(String operation, String field) {
        return new Accumulator(operation, field);
    }

    /**
     * Defines an accumulator for use in an aggregation pipeline.
     *
     * @param operation the accumulator operation
     * @param field     the field to use
     * @return an Accumulator
     */
    public static Accumulator accumulator(String operation, Object field) {
        return new Accumulator(operation, field);
    }

    /**
     * @return the operation for this accumulator
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @return the value for this accumulator
     */
    public Object getValue() {
        return value;
    }

    @Override
    public Document toDocument() {
        Document document = new Document();
        if (value instanceof List) {
            List<Object> dbValue = new ArrayList<>();
            for (Object o : (List) value) {
                if (o instanceof AggregationElement) {
                    dbValue.add(((AggregationElement) o).toDocument());
                } else {
                    dbValue.add(o);
                }
            }
            document.put(operation, dbValue);
        } else if (value instanceof AggregationElement) {
            document.put(operation, ((AggregationElement) value).toDocument());
        } else {
            document.put(operation, value);
        }

        return document;
    }
}
