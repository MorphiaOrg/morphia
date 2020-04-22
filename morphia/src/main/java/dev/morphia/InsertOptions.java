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
 */
public class InsertOptions {
    private com.mongodb.InsertOptions options = new com.mongodb.InsertOptions();

    /**
     * Creates a new options instance.
     */
    public InsertOptions() {
    }

    /**
     * Create a copy of the options instance.
     *
     * @return the copy
     */
    public InsertOptions copy() {
        return new InsertOptions()
            .bypassDocumentValidation(getBypassDocumentValidation())
            .continueOnError(isContinueOnError())
            .writeConcern(getWriteConcern());
    }

    com.mongodb.InsertOptions getOptions() {
        return options;
    }

    /**
     * Set the write concern to use for the insert.
     *
     * @param writeConcern the write concern
     * @return this
     */
    public InsertOptions writeConcern(final WriteConcern writeConcern) {
        options.writeConcern(writeConcern);
        return this;
    }

    /**
     * Set whether documents will continue to be inserted after a failure to insert one.
     *
     * @param continueOnError whether to continue on error
     * @return this
     * @deprecated this value is not supported in 2.0
     */
    @Deprecated
    public InsertOptions continueOnError(final boolean continueOnError) {
        options.continueOnError(continueOnError);
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
     * Whether documents will continue to be inserted after a failure to insert one (most commonly due to a duplicate key error).  Note that
     * this only is relevant for multi-document inserts. The default value is false.
     *
     * @return whether insertion will continue on error.
     * @deprecated this value is not supported in 2.0
     */
    @Deprecated
    public boolean isContinueOnError() {
        return options.isContinueOnError();
    }

    /**
     * Gets whether to bypass document validation, or null if unspecified.  The default is null.
     *
     * @return whether to bypass document validation, or null if unspecified.
     * @mongodb.server.release 3.2
     */
    public Boolean getBypassDocumentValidation() {
        return options.getBypassDocumentValidation();
    }

    /**
     * Sets whether to bypass document validation.
     *
     * @param bypassDocumentValidation whether to bypass document validation, or null if unspecified
     * @return this
     * @mongodb.server.release 3.2
     */
    public InsertOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        options.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }
}
