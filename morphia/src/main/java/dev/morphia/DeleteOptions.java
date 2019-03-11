/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.mongodb.client.model.DBCollectionRemoveOptions;

/**
 * The options to apply when removing documents from the DBCollection
 *
 * @since 1.3
 * @mongodb.driver.manual tutorial/remove-documents/ Remove Documents
 */
public final class DeleteOptions {
    private final DBCollectionRemoveOptions options = new DBCollectionRemoveOptions();

    /**
     * Creates a new options instance.
     */
    public DeleteOptions() {
    }

    /**
     * Copies this instance to a new one.
     *
     * @return the new instance
     */
    public DeleteOptions copy() {
        DeleteOptions deleteOptions = new DeleteOptions()
            .writeConcern(getWriteConcern());

        if (getCollation() != null) {
            deleteOptions.collation(Collation.builder(getCollation()).build());
        }

        return deleteOptions;
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
    public DeleteOptions collation(final Collation collation) {
        options.collation(collation);
        return this;
    }

    /**
     * The write concern to use for the delete.
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
    public DeleteOptions writeConcern(final WriteConcern writeConcern) {
        options.writeConcern(writeConcern);
        return this;
    }

    DBCollectionRemoveOptions getOptions() {
        return options;
    }
}
