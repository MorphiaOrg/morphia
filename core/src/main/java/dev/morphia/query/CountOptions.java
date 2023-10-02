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

package dev.morphia.query;

import java.util.concurrent.TimeUnit;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.ReadConfigurable;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * The options for a count operation.
 *
 * @mongodb.driver.manual reference/command/count/ Count
 * @since 1.3
 */
public class CountOptions extends com.mongodb.client.model.CountOptions
        implements CollectionConfigurable<CountOptions>, ReadConfigurable<CountOptions> {
    private ReadPreference readPreference;
    private ReadConcern readConcern;
    private ClientSession clientSession;
    private String collection;

    @Override
    public CountOptions collection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String collection() {
        return collection;
    }

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public CountOptions hint(String hint) {
        super.hint(new Document(hint, 1));
        return this;
    }

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public CountOptions hint(Document hint) {
        super.hint(hint);
        return this;
    }

    @Override
    public CountOptions hint(@Nullable Bson hint) {
        super.hint(hint);
        return this;
    }

    @Override
    public CountOptions hintString(@Nullable String hint) {
        super.hintString(hint);
        return this;
    }

    @Override
    public CountOptions limit(int limit) {
        super.limit(limit);
        return this;
    }

    @Override
    public CountOptions skip(int skip) {
        super.skip(skip);
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    @Deprecated(forRemoval = true, since = "2.3")
    public long getMaxTime(TimeUnit timeUnit) {
        return super.getMaxTime(timeUnit);
    }

    @Override
    public CountOptions maxTime(long maxTime, TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public CountOptions collation(@Nullable Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * @return this
     * @since 2.3
     */
    @Override
    public CountOptions comment(String comment) {
        super.comment(comment);
        return this;
    }

    /**
     * @return this
     * @since 2.3
     */
    @Override
    public CountOptions comment(BsonValue comment) {
        super.comment(comment);
        return this;
    }

    /**
     * Returns the readConcern
     *
     * @return the readConcern
     * @mongodb.server.release 3.2
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public ReadConcern readConcern() {
        return readConcern;
    }

    /**
     * Returns the readPreference
     *
     * @return the readPreference
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public ReadPreference readPreference() {
        return readPreference;
    }

    /**
     * Sets the readConcern
     *
     * @param readConcern the readConcern
     * @return this
     * @mongodb.server.release 3.2
     */
    @Override
    public CountOptions readConcern(ReadConcern readConcern) {
        this.readConcern = readConcern;
        return this;
    }

    /**
     * Sets the readPreference
     *
     * @param readPreference the readPreference
     * @return this
     */
    @Override
    public CountOptions readPreference(ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }
}
