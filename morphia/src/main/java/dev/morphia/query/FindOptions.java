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
import com.mongodb.assertions.Assertions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Collation;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.StringJoiner;
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
    private Projection projection;
    private String queryLogId;
    private ClientSession clientSession;

    /**
     * Creates an instance with default values
     */
    public FindOptions() {
    }

    /**
     * Creates an copy of the given options
     *
     * @param original the orginal to copy
     */
    FindOptions(final FindOptions original) {
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
        this.projection = original.projection;
        this.queryLogId = original.queryLogId;
    }

    /**
     * Sets the limit
     *
     * @param limit the limit
     * @return this
     */
    public FindOptions limit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets how many documents to skip
     *
     * @param skip the count
     * @return this
     */
    public FindOptions skip(final int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * @param timeUnit the time unit to apply
     * @return the max time for the operation
     */
    public long getMaxTime(final TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        return timeUnit.convert(this.maxTimeMS, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets the max time
     *
     * @param maxTime  the max
     * @param timeUnit the unit
     * @return this
     */
    public FindOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        Assertions.isTrueArgument("maxTime > = 0", maxTime >= 0L);
        this.maxTimeMS = TimeUnit.MILLISECONDS.convert(maxTime, timeUnit);
        return this;
    }

    /**
     * @param timeUnit the time unit to apply
     * @return the max await time for the operation
     */
    public long getMaxAwaitTime(final TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        return timeUnit.convert(this.maxAwaitTimeMS, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets the max await time
     *
     * @param maxAwaitTime the max
     * @param timeUnit     the unit
     * @return this
     */
    public FindOptions maxAwaitTime(final long maxAwaitTime, final TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        Assertions.isTrueArgument("maxAwaitTime > = 0", maxAwaitTime >= 0L);
        this.maxAwaitTimeMS = TimeUnit.MILLISECONDS.convert(maxAwaitTime, timeUnit);
        return this;
    }

    /**
     * Sets the batch size
     *
     * @param batchSize the size
     * @return this
     */
    public FindOptions batchSize(final int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * @return the projection
     */
    public Projection projection() {
        if (projection == null) {
            projection = new Projection(this);
        }
        return projection;
    }

    /**
     * Sets to the sort to use
     *
     * @param sort the sort document
     * @return this
     */
    public FindOptions sort(final Document sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sets whether to disable cursor time out
     *
     * @param noCursorTimeout true if the time should be disabled
     * @return this
     */
    public FindOptions noCursorTimeout(final boolean noCursorTimeout) {
        this.noCursorTimeout = noCursorTimeout;
        return this;
    }

    /**
     * Users should not set this under normal circumstances.
     *
     * @param oplogReplay if oplog replay is enabled
     * @return this
     */
    public FindOptions oplogReplay(final boolean oplogReplay) {
        this.oplogReplay = oplogReplay;
        return this;
    }

    /**
     * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
     *
     * @param partial if partial results for sharded clusters is enabled
     * @return this
     */
    public FindOptions partial(final boolean partial) {
        this.partial = partial;
        return this;
    }

    /**
     * Sets the cursor type
     *
     * @param cursorType the type
     * @return this
     */
    public FindOptions cursorType(final CursorType cursorType) {
        this.cursorType = Assertions.notNull("cursorType", cursorType);
        return this;
    }

    /**
     * Sets the collation to use
     *
     * @param collation the collation
     * @return this
     */
    public FindOptions collation(final Collation collation) {
        this.collation = collation;
        return this;
    }

    /**
     * Sets the index hint
     *
     * @param hint the hint
     * @return this
     */
    public FindOptions hint(final Document hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public FindOptions hint(final String hint) {
        hint(new Document(hint, 1));
        return this;
    }

    /**
     * Sets the max index value
     *
     * @param max the max
     * @return this
     */
    public FindOptions max(final Document max) {
        this.max = max;
        return this;
    }

    /**
     * Sets the min index value
     *
     * @param min the min
     * @return this
     */
    public FindOptions min(final Document min) {
        this.min = min;
        return this;
    }

    /**
     * Sets if only the key value should be returned
     *
     * @param returnKey true if only the key should be returned
     * @return this
     */
    public FindOptions returnKey(final boolean returnKey) {
        this.returnKey = returnKey;
        return this;
    }

    /**
     * Sets if the record ID should be returned
     *
     * @param showRecordId true if the record id should be returned
     * @return this
     */
    public FindOptions showRecordId(final boolean showRecordId) {
        this.showRecordId = showRecordId;
        return this;
    }

    /**
     * Sets the read preference to apply
     *
     * @param readPreference the read preference
     * @return this
     */
    public FindOptions readPreference(final ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    /**
     * @param query    the query to execute
     * @param iterable the iterable to use
     * @param mapper   the mapper to use
     * @param type     the result type
     * @param <T>      the result type
     * @return the iterable instance for the query results
     * @morphia.internal
     */
    public <T> FindIterable<T> apply(final QueryImpl query, final FindIterable<T> iterable, final Mapper mapper, final Class type) {
        Document fieldsObject = query.getFieldsObject();
        if (fieldsObject != null) {
            iterable.projection(fieldsObject);
        } else if (projection != null) {
            iterable.projection(projection.map(mapper, type));
        }

        iterable.batchSize(batchSize);
        iterable.collation(collation);
        iterable.comment(comment);
        if (cursorType != null) {
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
        if (querySort != null) {
            iterable.sort(querySort);
        } else if (sort != null) {
            iterable.sort(sort);
        }
        return iterable;
    }

    @Override
    public int hashCode() {
        int result = getBatchSize();
        result = 31 * result + getLimit();
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
        return getProjection() != null ? getProjection().equals(that.getProjection()) : that.getProjection() == null;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FindOptions.class.getSimpleName() + "[", "]")
                   .add("batchSize=" + batchSize)
                   .add("limit=" + limit)
                   .add("modifiers=" + modifiers)
                   .add("maxTimeMS=" + maxTimeMS)
                   .add("maxAwaitTimeMS=" + maxAwaitTimeMS)
                   .add("skip=" + skip)
                   .add("sort=" + sort)
                   .add("cursorType=" + cursorType)
                   .add("noCursorTimeout=" + noCursorTimeout)
                   .add("oplogReplay=" + oplogReplay)
                   .add("partial=" + partial)
                   .add("collation=" + collation)
                   .add("comment='" + comment + "'")
                   .add("hint=" + hint)
                   .add("max=" + max)
                   .add("min=" + min)
                   .add("returnKey=" + returnKey)
                   .add("showRecordId=" + showRecordId)
                   .add("snapshot=" + snapshot)
                   .add("readPreference=" + readPreference)
                   .add("projection=" + projection)
                   .add("queryLogId='" + queryLogId + "'")
                   .toString();
    }

    /**
     * @return the batch size
     */
    public int getBatchSize() {
        return this.batchSize;
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return this.limit;
    }

    /**
     * @return the skip count
     */
    public int getSkip() {
        return this.skip;
    }

    /**
     * @return the sort criteria
     */
    public Document getSort() {
        return this.sort;
    }

    /**
     * @return the cursor type
     */
    public CursorType getCursorType() {
        return this.cursorType;
    }

    /**
     * @return is the cursor timeout enabled
     */
    public boolean isNoCursorTimeout() {
        return this.noCursorTimeout;
    }

    /**
     * @return is oplog replay enabled
     */
    public boolean isOplogReplay() {
        return this.oplogReplay;
    }

    /**
     * @return are partial results enabled
     */
    public boolean isPartial() {
        return this.partial;
    }

    /**
     * @return the collation
     */
    public Collation getCollation() {
        return this.collation;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * @return the index hint
     */
    public Document getHint() {
        return this.hint;
    }

    /**
     * @return the max value
     */
    public Document getMax() {
        return this.max;
    }

    /**
     * @return the min value
     */
    public Document getMin() {
        return this.min;
    }

    /**
     * @return is return key only enabled
     */
    public boolean isReturnKey() {
        return this.returnKey;
    }

    /**
     * @return is showing the record id enabled
     */
    public boolean isShowRecordId() {
        return this.showRecordId;
    }

    /**
     * @return the read preference
     */
    public ReadPreference getReadPreference() {
        return readPreference;
    }

    /**
     * @return the projection
     */
    public Projection getProjection() {
        return this.projection;
    }

    /**
     * @return a copy of this instance
     */
    public FindOptions copy() {
        return new FindOptions(this);
    }

    /**
     * This is an experimental method.  It's implementation and presence are subject to change.
     *
     * @return this
     * @morphia.internal
     */
    public FindOptions logQuery() {
        queryLogId = new ObjectId().toString();
        comment(Sofia.loggedQuery(queryLogId));
        return this;
    }

    /**
     * Sets the comment to log with the query
     *
     * @param comment the comment
     * @return this
     */
    public FindOptions comment(final String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * This is an experimental method.  It's implementation and presence are subject to change.
     *
     * @return this
     * @morphia.internal
     */
    public boolean isLogQuery() {
        return queryLogId != null;
    }

    /**
     * @return the query log id used for retrieving the logged query
     * @morphia.internal
     */
    public String getQueryLogId() {
        return queryLogId;
    }

    /**
     * Set the client session to use for the insert.
     *
     * @param clientSession the client session
     * @return this
     * @since 2.0
     */
    public FindOptions clientSession(final ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    /**
     * The client session to use for the insertion.
     *
     * @return the client session
     * @since 2.0
     */
    public ClientSession clientSession() {
        return clientSession;
    }
}
