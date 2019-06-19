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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The options for find and modify operations.
 *
 * @since 1.3
 */
public final class FindAndModifyOptions extends FindOneAndUpdateOptions {
    private WriteConcern writeConcern;

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public void writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    /**
     * Creates a new options instance.
     */
    public FindAndModifyOptions() {
    }

    @Override
    public FindAndModifyOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
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
    public FindAndModifyOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    @Override
    public FindAndModifyOptions arrayFilters(final List<? extends Bson> arrayFilters) {
        super.arrayFilters(arrayFilters);
        return this;
    }
}
