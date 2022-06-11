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
import com.mongodb.client.ClientSession;
import com.mongodb.lang.Nullable;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.SessionConfigurable;
import dev.morphia.internal.WriteConfigurable;
import org.bson.BsonValue;

/**
 * Options related to insertion of documents into MongoDB.  The setter methods return {@code this} so that a chaining style can be used.
 *
 * @since 1.3
 */
public class InsertOneOptions implements SessionConfigurable<InsertOneOptions>, WriteConfigurable<InsertOneOptions>,
                                             CollectionConfigurable<InsertOneOptions> {
    private com.mongodb.client.model.InsertOneOptions options = new com.mongodb.client.model.InsertOneOptions();
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    private ClientSession clientSession;
    private boolean unset;
    private String collection;

    /**
     * Creates a new options wrapper
     */
    public InsertOneOptions() {
    }

    /**
     * @param that the options to copy
     * @morphia.internal
     * @since 2.0
     */
    @MorphiaInternal
    public InsertOneOptions(InsertOneOptions that) {
        this.options = that.options;
        this.writeConcern = that.writeConcern;
        this.clientSession = that.clientSession;
    }

    /**
     * @return the bypass document level validation flag
     * @morphia.internal
     * @see com.mongodb.client.model.InsertOneOptions#getBypassDocumentValidation()
     */
    @Nullable
    @MorphiaInternal
    public Boolean bypassDocumentValidation() {
        return options.getBypassDocumentValidation();
    }

    /**
     * Sets whether to bypass document validation.
     *
     * @param bypassDocumentValidation whether to bypass document validation, or null if unspecified
     * @return this
     * @mongodb.server.release 3.2
     */
    public InsertOneOptions bypassDocumentValidation(@Nullable Boolean bypassDocumentValidation) {
        options.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    @Override
    public InsertOneOptions clientSession(@Nullable ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    public InsertOneOptions collection(@Nullable String collection) {
        this.collection = collection;
        return this;
    }

    public String collection() {
        return collection;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.InsertOneOptions#comment(String)
     * @since 2.3
     */
    public InsertOneOptions comment(String comment) {
        options.comment(comment);
        return this;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.InsertOneOptions#comment(BsonValue)
     * @since 2.3
     */
    public InsertOneOptions comment(BsonValue comment) {
        options.comment(comment);
        return this;
    }

    /**
     * Gets the the bypass document level validation flag
     *
     * @return the bypass document level validation flag
     */
    @Nullable
    @Deprecated(forRemoval = true, since = "2.3")
    public Boolean getBypassDocumentValidation() {
        return bypassDocumentValidation();
    }

    /**
     * @return the driver version of the options
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public com.mongodb.client.model.InsertOneOptions getOptions() {
        return options;
    }

    /**
     * @return the driver version of the options
     * @morphia.internal
     */
    @MorphiaInternal
    public com.mongodb.client.model.InsertOneOptions options() {
        return options;
    }

    /**
     * Applies the rules for storing null/empty values for fields not present in the object to be merged.
     *
     * @return this true if the rules for storing null/empty values should be applied
     * @since 2.2
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public boolean unsetMissing() {
        return unset;
    }

    /**
     * Applies the rules for storing null/empty values for fields no present in the object to be merged.
     *
     * @param unset true if the rules should be applied
     * @return this
     * @since 2.2
     */
    public InsertOneOptions unsetMissing(boolean unset) {
        this.unset = unset;
        return this;
    }

    /**
     * Set the write concern to use for the insert.
     *
     * @param writeConcern the write concern
     * @return this
     */
    public InsertOneOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * The write concern to use for the insertion.  By default the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     */
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
