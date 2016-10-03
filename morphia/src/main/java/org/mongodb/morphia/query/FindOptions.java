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

package org.mongodb.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.DBObject;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DBCollectionFindOptions;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("CheckStyle")
public class FindOptions {
    private DBCollectionFindOptions options = new DBCollectionFindOptions();

    public FindOptions() {
    }

    FindOptions(final DBCollectionFindOptions copy) {
        options = copy;
    }

    public FindOptions batchSize(final int batchSize) {
        options.batchSize(batchSize);
        return this;

    }

    public FindOptions collation(final Collation collation) {
        options.collation(collation);
        return this;
    }

    public FindOptions copy() {
        return new FindOptions(options.copy());
    }

    public FindOptions cursorType(final CursorType cursorType) {
        options.cursorType(cursorType);
        return this;
    }

    public int getBatchSize() {
        return options.getBatchSize();
    }

    public Collation getCollation() {
        return options.getCollation();
    }

    public CursorType getCursorType() {
        return options.getCursorType();
    }

    public int getLimit() {
        return options.getLimit();
    }

    public long getMaxAwaitTime(final TimeUnit timeUnit) {
        return options.getMaxAwaitTime(timeUnit);
    }

    public long getMaxTime(final TimeUnit timeUnit) {
        return options.getMaxTime(timeUnit);
    }

    BasicDBObject getModifiersDBObject() {
        return (BasicDBObject) options.getModifiers();
    }

    public DBObject getProjection() {
        return options.getProjection();
    }

    public ReadConcern getReadConcern() {
        return options.getReadConcern();
    }

    public ReadPreference getReadPreference() {
        return options.getReadPreference();
    }

    public int getSkip() {
        return options.getSkip();
    }

    DBObject getSortDBObject() {
        return options.getSort();
    }

    public boolean isNoCursorTimeout() {
        return options.isNoCursorTimeout();
    }

    public boolean isOplogReplay() {
        return options.isOplogReplay();
    }

    public boolean isPartial() {
        return options.isPartial();
    }

    public FindOptions limit(final int limit) {
        options.limit(limit);
        return this;
    }

    public FindOptions maxAwaitTime(final long maxAwaitTime, final TimeUnit timeUnit) {
        options.maxAwaitTime(maxAwaitTime, timeUnit);
        return this;
    }

    public FindOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        options.maxTime(maxTime, timeUnit);
        return this;
    }

    public FindOptions modifiers(final DBObject modifiers) {
        options.modifiers(modifiers);
        return this;

    }

    public FindOptions noCursorTimeout(final boolean noCursorTimeout) {
        options.noCursorTimeout(noCursorTimeout);
        return this;

    }

    public FindOptions oplogReplay(final boolean oplogReplay) {
        options.oplogReplay(oplogReplay);
        return this;

    }

    public FindOptions partial(final boolean partial) {
        options.partial(partial);
        return this;

    }

    public FindOptions projection(final DBObject projection) {
        options.projection(projection);
        return this;

    }

    public FindOptions readConcern(final ReadConcern readConcern) {
        options.readConcern(readConcern);
        return this;

    }

    public FindOptions readPreference(final ReadPreference readPreference) {
        options.readPreference(readPreference);
        return this;

    }

    public FindOptions skip(final int skip) {
        options.skip(skip);
        return this;
    }

    public FindOptions sort(final DBObject sort) {
        options.sort(sort);
        return this;

    }

    DBCollectionFindOptions toDriverOptions() {
        return options;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindOptions)) {
            return false;
        }

        final FindOptions that = (FindOptions) o;

        return getBatchSize() == that.getBatchSize()
            && getLimit() == that.getLimit()
            && getModifiersDBObject().equals(that.getModifiersDBObject())
            && getMaxTime(TimeUnit.MILLISECONDS) == that.getMaxTime(TimeUnit.MILLISECONDS)
            && getMaxAwaitTime(TimeUnit.MILLISECONDS) == that.getMaxAwaitTime(TimeUnit.MILLISECONDS)
            && getSkip() == that.getSkip()
            && getCursorType() == that.getCursorType()
            && isNoCursorTimeout() == that.isNoCursorTimeout()
            && isOplogReplay() == that.isOplogReplay()
            && isPartial() == that.isPartial()
            && (getSortDBObject() == null && that.getSortDBObject() == null || getSortDBObject().equals(that.getSortDBObject()))
            && (getReadPreference() == null && that.getReadPreference() == null || getReadPreference().equals(that.getReadPreference()))
            && (getReadConcern() == null && that.getReadConcern() == null || getReadConcern().equals(that.getReadConcern()))
            && (getProjection() == null && that.getProjection() == null || getProjection().equals(that.getProjection()))
            && (getCollation() == null && that.getCollation() == null || getCollation().equals(that.getCollation()));

    }

    @Override
    public int hashCode() {
        return options.hashCode();
    }
}
