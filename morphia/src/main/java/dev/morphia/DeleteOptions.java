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
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import dev.morphia.internal.SessionConfigurable;

/**
 * The options to apply when removing documents from the MongoCollection
 *
 * @mongodb.driver.manual tutorial/remove-documents/ Remove Documents
 * @since 1.3
 */
public final class DeleteOptions extends com.mongodb.client.model.DeleteOptions implements SessionConfigurable<DeleteOptions> {
    private boolean multi;
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    private ClientSession clientSession;

    /**
     * Creates a new options instance
     */
    public DeleteOptions() {
    }

    /**
     * @param that the options to copy
     * @morphia.internal
     * @since 2.0
     */
    public DeleteOptions(final DeleteOptions that) {
        this.multi = that.multi;
        this.writeConcern = that.writeConcern;
        this.clientSession = that.clientSession;
    }

    /**
     * Applies the options to the collection
     *
     * @param collection the collection to update
     * @param <T>        the collection type
     * @return either the passed collection or the updated collection
     * @since 2.0
     */
    public <T> MongoCollection<T> apply(final MongoCollection<T> collection) {
        return writeConcern == null
               ? collection
               : collection.withWriteConcern(writeConcern);
    }

    @Override
    public DeleteOptions clientSession(final ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
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
     * @deprecated use {@link #writeConcern()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

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

    /**
     * The write concern to use for the delete.
     *
     * @return the write concern, or null if the default will be used.
     */
    public WriteConcern writeConcern() {
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
