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

/**
 * The options to apply when removing documents from the MongoCollection
 *
 * @mongodb.driver.manual tutorial/remove-documents/ Remove Documents
 * @since 1.3
 */
public final class DeleteOptions extends com.mongodb.client.model.DeleteOptions {
    private boolean multi;
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;

    /**
     * @return is this delete for multiple documents
     */
    public boolean isMulti() {
        return multi;
    }

    /**
     * @param multi true if this delete should affect multiple documents
     * @return this
     */
    public DeleteOptions multi(final boolean multi) {
        this.multi = multi;
        return this;
    }

    @Override
    public DeleteOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * The write concern to use for the delete.
     *
     * @return the write concern, or null if the default will be used.
     */
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * The write concern to use for the delete.
     *
     * @return the write concern, or null if the default will be used.
     * @deprecated use {@link #writeConcern()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public DeleteOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }
}
