/*
 * Copyright 2016 MongoDB, Inc.
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

package dev.morphia;

import com.mongodb.WriteConcern;

/**
 * Options related to insertion of documents into MongoDB.  The setter methods return {@code this} so that a chaining style can be used.
 *
 * @since 1.3
 * @deprecated use {@link dev.morphia.InsertOneOptions} or {@link dev.morphia.InsertManyOptions} instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public class InsertOptions {
    private WriteConcern writeConcern;
    private boolean ordered = true;
    private Boolean bypassDocumentValidation;

    InsertOneOptions toInsertOneOptions() {
        return new InsertOneOptions()
            .bypassDocumentValidation(bypassDocumentValidation);
    }

    InsertManyOptions toInsertManyOptions() {
        return new InsertManyOptions()
            .bypassDocumentValidation(bypassDocumentValidation)
            .ordered(ordered);
    }

    /**
     * Set the write concern to use for the insert.
     *
     * @param writeConcern the write concern
     * @return this
     */
    public InsertOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * The write concern to use for the insertion.  By default the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     */
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    /**
     * Gets whether to bypass document validation, or null if unspecified.  The default is null.
     *
     * @return whether to bypass document validation, or null if unspecified.
     * @mongodb.server.release 3.2
     */
    public Boolean getBypassDocumentValidation() {
        return bypassDocumentValidation;
    }

    /**
     * Sets whether to bypass document validation.
     *
     * @param bypassDocumentValidation whether to bypass document validation, or null if unspecified
     * @return this
     * @mongodb.server.release 3.2
     */
    public InsertOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = bypassDocumentValidation;
        return this;
    }

    /**
     * Sets whether the server should insert the documents in the order provided.
     *
     * @param ordered true if documents should be inserted in order
     * @return this
     */
    public InsertOptions ordered(final boolean ordered) {
        this.ordered = ordered;
        return this;
    }

    /**
     * Gets whether the documents should be inserted in the order provided, stopping on the first failed insertion. The default is true.
     * If false, the server will attempt to insert all the documents regardless of an failures.
     *
     * @return whether the the documents should be inserted in order
     */
    public boolean isOrdered() {
        return ordered;
    }
}
