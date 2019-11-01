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

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

/**
 * The options for a count operation.
 *
 * @mongodb.driver.manual reference/command/count/ Count
 * @since 1.3
 */
public class CountOptions extends com.mongodb.client.model.CountOptions {
    private ReadPreference readPreference;
    private ReadConcern readConcern;

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public CountOptions hint(final String hint) {
        super.hint(new Document(hint, 1));
        return this;
    }

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public CountOptions hint(final Document hint) {
        super.hint(hint);
        return this;
    }

    @Override
    public CountOptions hint(final Bson hint) {
        super.hint(hint);
        return this;
    }

    @Override
    public CountOptions limit(final int limit) {
        super.limit(limit);
        return this;
    }


    @Override
    public CountOptions skip(final int skip) {
        super.skip(skip);
        return this;
    }

    @Override
    public long getMaxTime(final TimeUnit timeUnit) {
        return super.getMaxTime(timeUnit);
    }

    @Override
    public CountOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public CountOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * Returns the readConcern
     *
     * @return the readConcern
     * @mongodb.server.release 3.2
     */
    public ReadConcern readConcern() {
        return readConcern;
    }

    /**
     * Sets the readConcern
     *
     * @param readConcern the readConcern
     * @return this
     * @mongodb.server.release 3.2
     */
    public CountOptions readConcern(final ReadConcern readConcern) {
        this.readConcern = readConcern;
        return this;
    }

    /**
     * Returns the readPreference
     *
     * @return the readPreference
     */
    public ReadPreference readPreference() {
        return readPreference;
    }

    /**
     * Sets the readPreference
     *
     * @param readPreference the readPreference
     * @return this
     */
    public CountOptions readPreference(final ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }
}

