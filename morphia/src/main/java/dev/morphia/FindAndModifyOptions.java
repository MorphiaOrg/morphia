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

import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DBCollectionFindAndModifyOptions;
import dev.morphia.query.Query;

import java.util.concurrent.TimeUnit;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * The options for find and modify operations.
 *
 * @since 1.3
 */
public final class FindAndModifyOptions {
    private DBCollectionFindAndModifyOptions options = new DBCollectionFindAndModifyOptions()
        .returnNew(true);

    /**
     * Creates a new options instance.
     */
    public FindAndModifyOptions() {
    }

    FindAndModifyOptions copy() {
        FindAndModifyOptions copy = new FindAndModifyOptions();
        copy.bypassDocumentValidation(getBypassDocumentValidation());
        copy.collation(getCollation());
        copy.maxTime(getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        copy.projection(getProjection());
        copy.remove(isRemove());
        copy.returnNew(isReturnNew());
        copy.sort(getSort());
        copy.update(getUpdate());
        copy.upsert(isUpsert());
        copy.writeConcern(getWriteConcern());
        return copy;
    }

    DBCollectionFindAndModifyOptions getOptions() {
        return copy().options;
    }

    DBObject getProjection() {
        return options.getProjection();
    }

    FindAndModifyOptions projection(final DBObject projection) {
        options.projection(projection);
        return this;
    }

    /**
     * Returns the sort
     *
     * @return the sort
     */
    DBObject getSort() {
        return options.getSort();
    }

    /**
     * Sets the sort
     *
     * @param sort the sort
     * @return this
     */
    FindAndModifyOptions sort(final DBObject sort) {
        options.sort(sort);
        return this;
    }

    /**
     * Returns the remove
     *
     * @return the remove
     */
    public boolean isRemove() {
        return options.isRemove();
    }

    /**
     * Indicates whether to remove the elements matching the query or not
     *
     * @param remove true if the matching elements should be deleted
     * @return this
     * @deprecated This will be removed in 2.0.  use {@link Datastore#findAndDelete(Query)} to remove items.
     */
    @Deprecated
    public FindAndModifyOptions remove(final boolean remove) {
        options.remove(remove);
        return this;
    }

    /**
     * Returns the update
     *
     * @return the update
     */
    DBObject getUpdate() {
        return options.getUpdate();
    }

    /**
     * Sets the update
     *
     * @param update the update
     * @return this
     */
    FindAndModifyOptions update(final DBObject update) {
        options.update(update);
        return this;
    }

    /**
     * Returns the upsert
     *
     * @return the upsert
     */
    public boolean isUpsert() {
        return options.isUpsert();
    }

    /**
     * Indicates that an upsert should be performed
     *
     * @param upsert the upsert
     * @return this
     * @mongodb.driver.manual reference/method/db.collection.update/#upsert-behavior upsert
     */
    public FindAndModifyOptions upsert(final boolean upsert) {
        options.upsert(upsert);
        return this;
    }

    /**
     * Returns the returnNew
     *
     * @return the returnNew
     */
    public boolean isReturnNew() {
        return options.returnNew();
    }

    /**
     * Sets the returnNew
     *
     * @param returnNew the returnNew
     * @return this
     */
    public FindAndModifyOptions returnNew(final boolean returnNew) {
        options.returnNew(returnNew);
        return this;
    }

    /**
     * Returns the bypassDocumentValidation
     *
     * @return the bypassDocumentValidation
     */
    public Boolean getBypassDocumentValidation() {
        return options.getBypassDocumentValidation();
    }

    /**
     * Sets the bypassDocumentValidation
     *
     * @param bypassDocumentValidation the bypassDocumentValidation
     * @return this
     */
    public FindAndModifyOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        options.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    /**
     * Gets the maximum execution time on the server for this operation.  The default is 0, which places no limit on the execution time.
     *
     * @param timeUnit the time unit to return the result in
     * @return the maximum execution time in the given time unit
     * @mongodb.driver.manual reference/method/cursor.maxTimeMS/#cursor.maxTimeMS Max Time
     */
    public long getMaxTime(final TimeUnit timeUnit) {
        notNull("timeUnit", timeUnit);
        return options.getMaxTime(timeUnit);
    }

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime  the max time
     * @param timeUnit the time unit, which may not be null
     * @return this
     * @mongodb.driver.manual reference/method/cursor.maxTimeMS/#cursor.maxTimeMS Max Time
     */
    public FindAndModifyOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        options.maxTime(maxTime, timeUnit);
        return this;
    }

    /**
     * Returns the writeConcern
     *
     * @return the writeConcern
     * @mongodb.server.release 3.2
     */
    public WriteConcern getWriteConcern() {
        return options.getWriteConcern();
    }

    /**
     * Sets the writeConcern
     *
     * @param writeConcern the writeConcern
     * @return this
     * @mongodb.server.release 3.2
     */
    public FindAndModifyOptions writeConcern(final WriteConcern writeConcern) {
        options.writeConcern(writeConcern);
        return this;
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
    public FindAndModifyOptions collation(final Collation collation) {
        options.collation(collation);
        return this;
    }
}
