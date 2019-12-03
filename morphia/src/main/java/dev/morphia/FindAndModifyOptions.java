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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import dev.morphia.internal.SessionConfigurable;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The options for find and modify operations.
 *
 * @since 1.3
 */
public final class FindAndModifyOptions extends FindOneAndUpdateOptions implements SessionConfigurable<FindAndModifyOptions> {
    private WriteConcern writeConcern;
    private ClientSession clientSession;

    /**
     * Applies the options to the collection
     *
     * @param collection the collection to update
     * @param <T>        the collection type
     * @return either the passed collection or the updated collection
     */
    public <T> MongoCollection<T> apply(final MongoCollection<T> collection) {
        return writeConcern == null
               ? collection
               : collection.withWriteConcern(writeConcern);
    }

    @Override
    public FindAndModifyOptions clientSession(final ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    /**
     * @return the write concern
     * @deprecated use {@link #writeConcern()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    @Override
    public FindAndModifyOptions projection(final Bson projection) {
        super.projection(projection);
        return this;
    }

    @Override
    public FindAndModifyOptions sort(final Bson sort) {
        super.sort(sort);
        return this;
    }

    @Override
    public FindAndModifyOptions upsert(final boolean upsert) {
        super.upsert(upsert);
        return this;
    }

    @Override
    public FindAndModifyOptions returnDocument(final ReturnDocument returnDocument) {
        super.returnDocument(returnDocument);
        return this;
    }

    @Override
    public FindAndModifyOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public FindAndModifyOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    @Override
    public FindAndModifyOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    @Override
    public FindAndModifyOptions arrayFilters(final List<? extends Bson> arrayFilters) {
        super.arrayFilters(arrayFilters);
        return this;
    }

    /**
     * @param writeConcern the write concern
     * @return this
     */
    public FindAndModifyOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @return the write concern to use
     */
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
