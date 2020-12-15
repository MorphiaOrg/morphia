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
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Collation;
import dev.morphia.internal.SessionConfigurable;
import dev.morphia.internal.WriteConfigurable;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * The options to apply when updating documents in the MongoCollection
 *
 * @mongodb.driver.manual tutorial/modify-documents/ Updates
 * @mongodb.driver.manual reference/operator/update/ Update Operators
 * @mongodb.driver.manual reference/command/update/ Update Command
 * @since 1.3
 */
public class UpdateOptions extends com.mongodb.client.model.UpdateOptions
    implements SessionConfigurable<UpdateOptions>, WriteConfigurable<UpdateOptions> {
    private WriteConcern writeConcern;
    private boolean multi;
    private ClientSession clientSession;

    /**
     * Adds a new array filter
     *
     * @param filter the new filter
     * @return this
     * @since 2.1
     */
    public UpdateOptions arrayFilter(Filter filter) {
        List<Bson> arrayFilters = new ArrayList<>();
        if (getArrayFilters() != null) {
            arrayFilters.addAll(getArrayFilters());
        }
        Document filterDoc = new Document(filter.getName(), filter.getValue());
        if (filter.isNot()) {
            filterDoc = new Document("$not", filterDoc);
        }
        arrayFilters.add(new Document(filter.getField(), filterDoc));
        arrayFilters(arrayFilters);

        return this;
    }

    @Override
    public UpdateOptions clientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    /**
     * @param hint the hint to apply
     * @return this
     * @see #hint(Bson)
     * @since 2.2
     */
    public UpdateOptions hint(Document hint) {
        super.hint(hint);
        return this;
    }

    /**
     * @return true if the update should affect all entities
     */
    public boolean isMulti() {
        return multi;
    }

    /**
     * If true, sets this update to affect all matched documents.
     *
     * @param multi true for multiple updates
     * @return this
     */
    public UpdateOptions multi(boolean multi) {
        this.multi = multi;
        return this;
    }

    @Override
    public UpdateOptions upsert(boolean upsert) {
        super.upsert(upsert);
        return this;
    }

    @Override
    public UpdateOptions bypassDocumentValidation(Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    @Override
    public UpdateOptions collation(Collation collation) {
        super.collation(collation);
        return this;
    }

    @Override
    public UpdateOptions arrayFilters(List<? extends Bson> arrayFilters) {
        super.arrayFilters(arrayFilters);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public UpdateOptions hint(Bson hint) {
        super.hint(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public UpdateOptions hintString(String hint) {
        super.hintString(hint);
        return this;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public UpdateOptions writeConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * The write concern to use for the insertion.  By default the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     */
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
