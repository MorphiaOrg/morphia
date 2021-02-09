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
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;
import dev.morphia.internal.SessionConfigurable;
import dev.morphia.internal.WriteConfigurable;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * The options to apply when removing documents from the MongoCollection
 *
 * @mongodb.driver.manual tutorial/remove-documents/ Remove Documents
 * @since 1.3
 */
public final class DeleteOptions extends com.mongodb.client.model.DeleteOptions implements SessionConfigurable<DeleteOptions>,
                                                                                               WriteConfigurable<DeleteOptions> {
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
    public DeleteOptions(DeleteOptions that) {
        this.multi = that.multi;
        this.writeConcern = that.writeConcern;
        this.clientSession = that.clientSession;
    }

    @Override
    public DeleteOptions clientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    @Override
    public DeleteOptions collation(@Nullable Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public DeleteOptions hint(@Nullable Bson hint) {
        super.hint(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public DeleteOptions hintString(@Nullable String hint) {
        super.hintString(hint);
        return this;
    }

    /**
     * @param hint the hint to apply
     * @return this
     * @see #hint(Bson)
     * @since 2.2
     */
    public DeleteOptions hint(Document hint) {
        super.hint(hint);
        return this;
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
    public DeleteOptions multi(boolean multi) {
        this.multi = multi;
        return this;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public DeleteOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * The write concern to use for the delete.
     *
     * @return the write concern, or null if the default will be used.
     */
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
