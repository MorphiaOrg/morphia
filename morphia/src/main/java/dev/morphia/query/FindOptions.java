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

import com.mongodb.CursorType;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.assertions.Assertions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Collation;
import dev.morphia.mapping.Mapper;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * The options to apply to a find operation (also commonly referred to as a query).
 *
 * @mongodb.driver.manual tutorial/query-documents/ Find
 * @mongodb.driver.manual ../meta-driver/latest/legacy/mongodb-wire-protocol/#op-query OP_QUERY
 * @since 1.3
 */
public final class FindOptions {
    private int batchSize;
    private int limit;
    private Document modifiers;
    private long maxTimeMS;
    private long maxAwaitTimeMS;
    private int skip;
    private Document sort;
    private CursorType cursorType;
    private boolean noCursorTimeout;
    private boolean oplogReplay;
    private boolean partial;
    private Collation collation;
    private String comment;
    private Document hint;
    private Document max;
    private Document min;
    private boolean returnKey;
    private boolean showRecordId;
    private boolean snapshot;
    private ReadPreference readPreference;
    private WriteConcern writeConcern;
    private Projection projection;

    public FindOptions() {
    }

    FindOptions(FindOptions original) {
        this.batchSize = original.batchSize;
        this.limit = original.limit;
        this.modifiers = original.modifiers;
        this.maxTimeMS = original.maxTimeMS;
        this.maxAwaitTimeMS = original.maxAwaitTimeMS;
        this.skip = original.skip;
        this.sort = original.sort;
        this.cursorType = original.cursorType;
        this.noCursorTimeout = original.noCursorTimeout;
        this.oplogReplay = original.oplogReplay;
        this.partial = original.partial;
        this.collation = original.collation;
        this.comment = original.comment;
        this.hint = original.hint;
        this.max = original.max;
        this.min = original.min;
        this.returnKey = original.returnKey;
        this.showRecordId = original.showRecordId;
        this.snapshot = original.snapshot;
        this.readPreference = original.readPreference;
        this.writeConcern = original.writeConcern;
        this.projection = original.projection;
    }

    public FindOptions limit(int limit) {
        this.limit = limit;
        return this;
    }

    public FindOptions skip(int skip) {
        this.skip = skip;
        return this;
    }

    public long getMaxTime(TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        return timeUnit.convert(this.maxTimeMS, TimeUnit.MILLISECONDS);
    }

    public FindOptions maxTime(long maxTime, TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        Assertions.isTrueArgument("maxTime > = 0", maxTime >= 0L);
        this.maxTimeMS = TimeUnit.MILLISECONDS.convert(maxTime, timeUnit);
        return this;
    }

    public long getMaxAwaitTime(TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        return timeUnit.convert(this.maxAwaitTimeMS, TimeUnit.MILLISECONDS);
    }

    public FindOptions maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        Assertions.isTrueArgument("maxAwaitTime > = 0", maxAwaitTime >= 0L);
        this.maxAwaitTimeMS = TimeUnit.MILLISECONDS.convert(maxAwaitTime, timeUnit);
        return this;
    }

    public FindOptions batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public Projection projection() {
        if (projection == null) {
            projection = new Projection(this);
        }
        return projection;
    }

    public FindOptions sort(Document sort) {
        this.sort = sort;
        return this;
    }

    public FindOptions noCursorTimeout(boolean noCursorTimeout) {
        this.noCursorTimeout = noCursorTimeout;
        return this;
    }

    public FindOptions oplogReplay(boolean oplogReplay) {
        this.oplogReplay = oplogReplay;
        return this;
    }

    public FindOptions partial(boolean partial) {
        this.partial = partial;
        return this;
    }

    public FindOptions cursorType(CursorType cursorType) {
        this.cursorType = Assertions.notNull("cursorType", cursorType);
        return this;
    }

    public FindOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    public FindOptions comment(String comment) {
        this.comment = comment;
        return this;
    }

    public FindOptions hint(Document hint) {
        this.hint = hint;
        return this;
    }

    public FindOptions max(Document max) {
        this.max = max;
        return this;
    }

    public FindOptions min(Document min) {
        this.min = min;
        return this;
    }

    public FindOptions returnKey(boolean returnKey) {
        this.returnKey = returnKey;
        return this;
    }

    public FindOptions showRecordId(boolean showRecordId) {
        this.showRecordId = showRecordId;
        return this;
    }

    public FindOptions readPreference(final ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    public FindOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @morphia.internal
     * @param query
     * @param iterable
     * @param mapper
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> FindIterable<T> apply(final QueryImpl query, final FindIterable<T> iterable, final Mapper mapper, final Class clazz) {
        Document fieldsObject = query.getFieldsObject();
        if(fieldsObject != null) {
            iterable.projection(fieldsObject);
        } else if(projection != null) {
            iterable.projection(projection.map(mapper, clazz));
        }

        iterable.batchSize(batchSize);
        iterable.collation(collation);
        iterable.comment(comment);
        if(cursorType != null) {
            iterable.cursorType(cursorType);
        }
        iterable.hint(hint);
        iterable.limit(limit);
        iterable.max(max);
        iterable.maxAwaitTime(maxAwaitTimeMS, TimeUnit.MILLISECONDS);
        iterable.maxTime(maxTimeMS, TimeUnit.MILLISECONDS);
        iterable.min(min);
        iterable.modifiers(modifiers);
        iterable.noCursorTimeout(noCursorTimeout);
        iterable.oplogReplay(oplogReplay);
        iterable.partial(partial);
        iterable.returnKey(returnKey);
        iterable.showRecordId(showRecordId);
        iterable.skip(skip);
        Document querySort = query.getSort();
        if(querySort != null) {
            iterable.sort(querySort);
        } else if(sort != null) {
            iterable.sort(sort);
        }
        return iterable;
    }

    @Override
    public int hashCode() {
        int result = getBatchSize();
        result = 31 * result + getLimit();
        result = 31 * result + (getModifiers() != null ? getModifiers().hashCode() : 0);
        result = 31 * result + (int) (maxTimeMS ^ (maxTimeMS >>> 32));
        result = 31 * result + (int) (maxAwaitTimeMS ^ (maxAwaitTimeMS >>> 32));
        result = 31 * result + getSkip();
        result = 31 * result + (getSort() != null ? getSort().hashCode() : 0);
        result = 31 * result + (getCursorType() != null ? getCursorType().hashCode() : 0);
        result = 31 * result + (isNoCursorTimeout() ? 1 : 0);
        result = 31 * result + (isOplogReplay() ? 1 : 0);
        result = 31 * result + (isPartial() ? 1 : 0);
        result = 31 * result + (getCollation() != null ? getCollation().hashCode() : 0);
        result = 31 * result + (getComment() != null ? getComment().hashCode() : 0);
        result = 31 * result + (getHint() != null ? getHint().hashCode() : 0);
        result = 31 * result + (getMax() != null ? getMax().hashCode() : 0);
        result = 31 * result + (getMin() != null ? getMin().hashCode() : 0);
        result = 31 * result + (isReturnKey() ? 1 : 0);
        result = 31 * result + (isShowRecordId() ? 1 : 0);
        result = 31 * result + (snapshot ? 1 : 0);
        result = 31 * result + (getReadPreference() != null ? getReadPreference().hashCode() : 0);
        result = 31 * result + (getWriteConcern() != null ? getWriteConcern().hashCode() : 0);
        result = 31 * result + (getProjection() != null ? getProjection().hashCode() : 0);
        return result;
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

        if (getBatchSize() != that.getBatchSize()) {
            return false;
        }
        if (getLimit() != that.getLimit()) {
            return false;
        }
        if (maxTimeMS != that.maxTimeMS) {
            return false;
        }
        if (maxAwaitTimeMS != that.maxAwaitTimeMS) {
            return false;
        }
        if (getSkip() != that.getSkip()) {
            return false;
        }
        if (isNoCursorTimeout() != that.isNoCursorTimeout()) {
            return false;
        }
        if (isOplogReplay() != that.isOplogReplay()) {
            return false;
        }
        if (isPartial() != that.isPartial()) {
            return false;
        }
        if (isReturnKey() != that.isReturnKey()) {
            return false;
        }
        if (isShowRecordId() != that.isShowRecordId()) {
            return false;
        }
        if (snapshot != that.snapshot) {
            return false;
        }
        if (getModifiers() != null ? !getModifiers().equals(that.getModifiers()) : that.getModifiers() != null) {
            return false;
        }
        if (getSort() != null ? !getSort().equals(that.getSort()) : that.getSort() != null) {
            return false;
        }
        if (getCursorType() != that.getCursorType()) {
            return false;
        }
        if (getCollation() != null ? !getCollation().equals(that.getCollation()) : that.getCollation() != null) {
            return false;
        }
        if (getComment() != null ? !getComment().equals(that.getComment()) : that.getComment() != null) {
            return false;
        }
        if (getHint() != null ? !getHint().equals(that.getHint()) : that.getHint() != null) {
            return false;
        }
        if (getMax() != null ? !getMax().equals(that.getMax()) : that.getMax() != null) {
            return false;
        }
        if (getMin() != null ? !getMin().equals(that.getMin()) : that.getMin() != null) {
            return false;
        }
        if (getReadPreference() != null ? !getReadPreference().equals(that.getReadPreference()) : that.getReadPreference() != null) {
            return false;
        }
        if (getWriteConcern() != null ? !getWriteConcern().equals(that.getWriteConcern()) : that.getWriteConcern() != null) {
            return false;
        }
        return getProjection() != null ? getProjection().equals(that.getProjection()) : that.getProjection() == null;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getSkip() {
        return this.skip;
    }

    public boolean isNoCursorTimeout() {
        return this.noCursorTimeout;
    }

    public boolean isOplogReplay() {
        return this.oplogReplay;
    }

    public boolean isPartial() {
        return this.partial;
    }

    public boolean isReturnKey() {
        return this.returnKey;
    }

    public boolean isShowRecordId() {
        return this.showRecordId;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Document getModifiers() {
        return this.modifiers;
    }

    public Document getSort() {
        return this.sort;
    }

    public CursorType getCursorType() {
        return this.cursorType;
    }

    public Collation getCollation() {
        return this.collation;
    }

    public String getComment() {
        return this.comment;
    }

    public Document getHint() {
        return this.hint;
    }

    public Document getMax() {
        return this.max;
    }

    public Document getMin() {
        return this.min;
    }

    public ReadPreference getReadPreference() {
        return readPreference;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public Projection getProjection() {
        return this.projection;
    }

    @Override
    public String toString() {
        return "FindOptions{" +
               "batchSize=" + batchSize +
               ", collation=" + collation +
               ", comment='" + comment + '\'' +
               ", cursorType=" + cursorType +
               ", hint=" + hint +
               ", limit=" + limit +
               ", max=" + max +
               ", maxAwaitTimeMS=" + maxAwaitTimeMS +
               ", maxTimeMS=" + maxTimeMS +
               ", min=" + min +
               ", modifiers=" + modifiers +
               ", noCursorTimeout=" + noCursorTimeout +
               ", oplogReplay=" + oplogReplay +
               ", partial=" + partial +
               ", projection=" + projection +
               ", readPreference=" + readPreference +
               ", returnKey=" + returnKey +
               ", showRecordId=" + showRecordId +
               ", skip=" + skip +
               ", snapshot=" + snapshot +
               ", sort=" + sort +
               ", writeConcern=" + writeConcern +
               '}';
    }

    public FindOptions copy() {
        return new FindOptions(this);
    }
}
