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
import com.mongodb.lang.Nullable;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * The options to apply to a find operation (also commonly referred to as a query).
 *
 * @since 1.3
 * @mongodb.driver.manual tutorial/query-documents/ Find
 * @mongodb.driver.manual ../meta-driver/latest/legacy/mongodb-wire-protocol/#op-query OP_QUERY
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
    private long maxScan;
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
        this.maxScan = original.maxScan;
        this.returnKey = original.returnKey;
        this.showRecordId = original.showRecordId;
        this.snapshot = original.snapshot;
        this.readPreference = original.readPreference;
        this.writeConcern = original.writeConcern;
        this.projection = original.projection;
    }
    public int getLimit() {
        return this.limit;
    }

    public FindOptions limit(int limit) {
        this.limit = limit;
        return this;
    }

    public int getSkip() {
        return this.skip;
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

    public int getBatchSize() {
        return this.batchSize;
    }

    public FindOptions batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /** @deprecated */
    @Deprecated
    @Nullable
    public Document getModifiers() {
        return this.modifiers;
    }

    /** @deprecated */
    @Deprecated
    public FindOptions modifiers(@Nullable Document modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public Projection projection() {
        if(projection != null){
            projection = new Projection(this);
        }
        return projection;
    }

    @Nullable
    public Projection getProjection() {
        return this.projection;
    }

    @Nullable
    public Document getSort() {
        return this.sort;
    }

    public FindOptions sort(@Nullable Document sort) {
        this.sort = sort;
        return this;
    }

    public boolean isNoCursorTimeout() {
        return this.noCursorTimeout;
    }

    public FindOptions noCursorTimeout(boolean noCursorTimeout) {
        this.noCursorTimeout = noCursorTimeout;
        return this;
    }

    public boolean isOplogReplay() {
        return this.oplogReplay;
    }

    public FindOptions oplogReplay(boolean oplogReplay) {
        this.oplogReplay = oplogReplay;
        return this;
    }

    public boolean isPartial() {
        return this.partial;
    }

    public FindOptions partial(boolean partial) {
        this.partial = partial;
        return this;
    }

    public CursorType getCursorType() {
        return this.cursorType;
    }

    public FindOptions cursorType(CursorType cursorType) {
        this.cursorType = (CursorType)Assertions.notNull("cursorType", cursorType);
        return this;
    }

    @Nullable
    public Collation getCollation() {
        return this.collation;
    }

    public FindOptions collation(@Nullable Collation collation) {
        this.collation = collation;
        return this;
    }

    @Nullable
    public String getComment() {
        return this.comment;
    }

    public FindOptions comment(@Nullable String comment) {
        this.comment = comment;
        return this;
    }

    @Nullable
    public Document getHint() {
        return this.hint;
    }

    public FindOptions hint(@Nullable Document hint) {
        this.hint = hint;
        return this;
    }

    @Nullable
    public Document getMax() {
        return this.max;
    }

    public FindOptions max(@Nullable Document max) {
        this.max = max;
        return this;
    }

    @Nullable
    public Document getMin() {
        return this.min;
    }

    public FindOptions min(@Nullable Document min) {
        this.min = min;
        return this;
    }

    public boolean isReturnKey() {
        return this.returnKey;
    }

    public FindOptions returnKey(boolean returnKey) {
        this.returnKey = returnKey;
        return this;
    }

    public boolean isShowRecordId() {
        return this.showRecordId;
    }

    public FindOptions showRecordId(boolean showRecordId) {
        this.showRecordId = showRecordId;
        return this;
    }

    public ReadPreference getReadPreference() {
        return readPreference;
    }

    public FindOptions readPreference(final ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public FindOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    public <T> FindIterable<T> apply(final FindIterable<T> iterable) {
        return iterable;
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
        if (maxScan != that.maxScan) {
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
        result = 31 * result + (int) (maxScan ^ (maxScan >>> 32));
        result = 31 * result + (isReturnKey() ? 1 : 0);
        result = 31 * result + (isShowRecordId() ? 1 : 0);
        result = 31 * result + (snapshot ? 1 : 0);
        result = 31 * result + (getReadPreference() != null ? getReadPreference().hashCode() : 0);
        result = 31 * result + (getWriteConcern() != null ? getWriteConcern().hashCode() : 0);
        result = 31 * result + (getProjection() != null ? getProjection().hashCode() : 0);
        return result;
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
               ", maxScan=" + maxScan +
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
