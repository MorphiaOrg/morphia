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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * The options to apply to a $push update operator.
 *
 * @mongodb.driver.manual reference/operator/update/push/ $push
 * @since 1.3
 */
public class PushOptions {
    private Integer position;
    private Integer slice;
    private Integer sort;
    private DBObject sortDocument;

    /**
     * Creates an empty options class
     */
    public PushOptions() {
    }

    /**
     * Sets the position for the update
     * @param position the position in the array for the update
     * @return this
     */
    public PushOptions position(final int position) {
        if (position < 0) {
            throw new UpdateException("The position must be at least 0.");
        }
        this.position = position;
        return this;
    }

    /**
     * Sets the slice value for the update
     * @param slice the slice value for the update
     * @return this
     */
    public PushOptions slice(final int slice) {
        this.slice = slice;
        return this;
    }

    /**
     * Sets the sort value for the update
     * @param sort the sort value for the update
     * @return this
     */
    public PushOptions sort(final int sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sets the sort value for the update
     *
     * @param field     the field to sort by
     * @param direction the direction of the sort
     * @return this
     */
    public PushOptions sort(final String field, final int direction) {
        if (sort != null) {
            throw new IllegalStateException("sortDocument can not be set if sort already is");
        }
        if (sortDocument == null) {
            sortDocument = new BasicDBObject();
        }
        sortDocument.put(field, direction);
        return this;
    }

    void update(final BasicDBObject dbObject) {
        if (position != null) {
            dbObject.put("$position", position);
        }
        if (slice != null) {
            dbObject.put("$slice", slice);
        }
        if (sort != null) {
            dbObject.put("$sort", sort);
        }
        if (sortDocument != null) {
            dbObject.put("$sort", sortDocument);
        }
    }

    /**
     * Helper method to create a PushOptions instance
     *
     * @return the new PushOptions instance
     */
    public static PushOptions options() {
        return new PushOptions();
    }
}
