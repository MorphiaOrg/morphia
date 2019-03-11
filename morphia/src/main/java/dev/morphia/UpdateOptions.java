/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
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
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DBCollectionUpdateOptions;

/**
 * The options to apply when updating documents in the DBCollection
 *
 * @mongodb.driver.manual tutorial/modify-documents/ Updates
 * @mongodb.driver.manual reference/operator/update/ Update Operators
 * @mongodb.driver.manual reference/command/update/ Update Command
 *
 * @since 1.3
 */
public class UpdateOptions {
    private DBCollectionUpdateOptions options = new DBCollectionUpdateOptions();

    DBCollectionUpdateOptions getOptions() {
        return options;
    }

    /**
     * Creates a new options instance.
     */
    public UpdateOptions() {
    }

    /**
     * Create a copy of the options instance.
     *
     * @return the copy
     */
    public UpdateOptions copy() {
        return new UpdateOptions()
            .bypassDocumentValidation(getBypassDocumentValidation())
            .collation(getCollation())
            .multi(isMulti())
            .upsert(isUpsert())
            .writeConcern(getWriteConcern());
    }

    /**
     * Returns true if a new document should be inserted if there are no matches to the query filter.  The default is false.
     *
     * @return true if a new document should be inserted if there are no matches to the query filter
     */
    public boolean isUpsert() {
        return options.isUpsert();
    }

    /**
     * Set to true if a new document should be inserted if there are no matches to the query filter.
     *
     * @param isUpsert true if a new document should be inserted if there are no matches to the query filter
     * @return this
     */
    public UpdateOptions upsert(final boolean isUpsert) {
        options.upsert(isUpsert);
        return this;
    }

    /**
     * Gets the the bypass document level validation flag
     *
     * @return the bypass document level validation flag
     * @mongodb.server.release 3.2
     */
    public Boolean getBypassDocumentValidation() {
        return options.getBypassDocumentValidation();
    }

    /**
     * Sets the bypass document level validation flag.
     *
     * @param bypassDocumentValidation If true, allows the write to opt-out of document level validation.
     * @return this
     * @mongodb.server.release 3.2
     */
    public UpdateOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        options.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    /**
     * Sets whether all documents matching the query filter will be removed.
     *
     * @param multi true if all documents matching the query filter will be removed
     * @return this
     */
    public UpdateOptions multi(final boolean multi) {
        options.multi(multi);
        return this;
    }

    /**
     * Gets whether all documents matching the query filter will be removed.  The default is true.
     *
     * @return whether all documents matching the query filter will be removed
     */
    public boolean isMulti() {
        return options.isMulti();
    }

    /**
     * Returns the collation options
     *
     * @return the collation options
     * @mongodb.server.release 3.4
     */
    public Collation getCollation() {
        return options.getCollation();
    }

    /**
     * Sets the collation
     *
     * @param collation the collation
     * @return this
     * @mongodb.server.release 3.4
     */
    public UpdateOptions collation(final Collation collation) {
        options.collation(collation);
        return this;
    }

    /**
     * The write concern to use for the insertion.  By default the write concern configured for the DBCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     */
    public WriteConcern getWriteConcern() {
        return options.getWriteConcern();
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public UpdateOptions writeConcern(final WriteConcern writeConcern) {
        options.writeConcern(writeConcern);
        return this;
    }
}
