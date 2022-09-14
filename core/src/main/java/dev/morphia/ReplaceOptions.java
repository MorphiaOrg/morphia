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
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;
import dev.morphia.internal.WriteConfigurable;
import org.bson.BsonValue;
import org.bson.conversions.Bson;

/**
 * Options related to insertion of documents into MongoDB. The setter methods return {@code this} so that a chaining style can be used.
 *
 * @since 2.3
 */
public class ReplaceOptions extends com.mongodb.client.model.ReplaceOptions implements WriteConfigurable<ReplaceOptions> {
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;

    /**
     * Creates a new options wrapper
     */
    public ReplaceOptions() {
    }

    @Override
    public ReplaceOptions upsert(boolean upsert) {
        super.upsert(upsert);
        return this;
    }

    /**
     * Sets whether to bypass document validation.
     *
     * @param bypassDocumentValidation whether to bypass document validation, or null if unspecified
     * @return this
     * @mongodb.server.release 3.2
     */
    public ReplaceOptions bypassDocumentValidation(@Nullable Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    @Override
    public ReplaceOptions collation(@Nullable Collation collation) {
        super.collation(collation);
        return this;
    }

    @Override
    public ReplaceOptions hint(@Nullable Bson hint) {
        super.hint(hint);
        return this;
    }

    @Override
    public ReplaceOptions hintString(@Nullable String hint) {
        super.hintString(hint);
        return this;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.ReplaceOptions#comment(String)
     */
    public ReplaceOptions comment(@Nullable String comment) {
        super.comment(comment);
        return this;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.ReplaceOptions#comment(BsonValue)
     */
    public ReplaceOptions comment(@Nullable BsonValue comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public ReplaceOptions let(Bson variables) {
        super.let(variables);
        return this;
    }

    /**
     * Sets the write concern to use for the insert.
     *
     * @param writeConcern the write concern
     * @return this
     */
    public ReplaceOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * The write concern to use for the insertion. By default, the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern or null if the default will be used.
     */
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
