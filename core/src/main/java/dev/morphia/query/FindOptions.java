package dev.morphia.query;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import com.mongodb.CursorType;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.assertions.Assertions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.cursor.TimeoutMode;
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.PathTarget;
import dev.morphia.internal.ReadConfigurable;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.NotMappableException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.sofia.Sofia;

import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * The options to apply to a find operation (also commonly referred to as a query).
 *
 * @mongodb.driver.manual tutorial/query-documents/ Find
 * @mongodb.driver.manual ../meta-driver/latest/legacy/mongodb-wire-protocol/#op-query OP_QUERY
 * @since 1.3
 */
@SuppressWarnings("deprecation")
public final class FindOptions implements ReadConfigurable<FindOptions>, CollectionConfigurable<FindOptions> {
    private Boolean allowDiskUse;
    private int batchSize;

    private Collation collation;

    private String collection;

    private BsonValue comment;

    private CursorType cursorType;

    private boolean disableValidation = false;

    private Document hint;

    private String hintString;

    private int limit;

    private Document max;

    private long maxAwaitTimeMS;

    private long maxTimeMS;

    private Document min;

    private boolean noCursorTimeout;

    private boolean partial;

    private Projection projection;

    private String queryLogId;

    private ReadConcern readConcern;

    private ReadPreference readPreference;

    private boolean returnKey;

    private boolean showRecordId;

    private int skip;

    private Document sort;

    private TimeoutMode timeoutMode;

    private Document variables;

    /**
     * Enables writing to temporary files on the server. When set to true, the server
     * can write temporary data to disk while executing the find operation.
     *
     * @param allowDiskUse true to allow disk use
     * @return this
     * @since 2.2
     */
    public FindOptions allowDiskUse(Boolean allowDiskUse) {
        this.allowDiskUse = allowDiskUse;
        return this;
    }

    /**
     * @param iterable the iterable to use
     * @param mapper   the mapper to use
     * @param type     the result type
     * @param <T>      the result type
     * @return the iterable instance for the query results
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public <T> FindIterable<T> apply(FindIterable<T> iterable, Mapper mapper, Class<?> type) {
        if (isLogQuery()) {
            logQuery(); //  reset to a new ID
        }
        if (projection != null) {
            projection.disableValidation(disableValidation);
            iterable.projection(projection.map(mapper, type));
        }

        iterable.allowDiskUse(allowDiskUse);
        iterable.batchSize(batchSize);
        iterable.collation(collation);
        iterable.comment(comment);
        if (cursorType != null) {
            iterable.cursorType(cursorType);
        }
        iterable.hint(hint);
        iterable.hintString(hintString);
        iterable.limit(limit);
        iterable.max(max);
        iterable.maxAwaitTime(maxAwaitTimeMS, TimeUnit.MILLISECONDS);
        iterable.maxTime(maxTimeMS, TimeUnit.MILLISECONDS);
        iterable.min(min);
        iterable.noCursorTimeout(noCursorTimeout);
        iterable.partial(partial);
        iterable.returnKey(returnKey);
        iterable.showRecordId(showRecordId);
        iterable.skip(skip);
        if (sort != null) {
            Document mapped = new Document();
            EntityModel model = null;
            try {
                model = mapper.getEntityModel(type);
            } catch (NotMappableException ignored) {
            }

            for (Entry<String, Object> entry : sort.entrySet()) {
                Object value = entry.getValue();
                boolean metaScore = value instanceof Document && ((Document) value).get("$meta") != null;
                mapped.put(new PathTarget(mapper, model, entry.getKey(), model != null && !metaScore).translatedPath(), value);
            }
            iterable.sort(mapped);
        }
        iterable.let(variables);
        if (timeoutMode != null) {
            iterable.timeoutMode(timeoutMode);
        }
        return iterable;
    }

    /**
     * This is an internal method. It's implementation and presence are subject to change.
     *
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean isLogQuery() {
        return queryLogId != null;
    }

    /**
     * This is an experimental method. It's implementation and presence are subject to change.
     *
     * @return this
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
    public FindOptions comment(String comment) {
        this.comment = new BsonString(comment);
        return this;
    }

    /**
     * Sets the batch size
     *
     * @param batchSize the size
     * @return this
     */
    public FindOptions batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Sets the collation to use
     *
     * @param collation the collation
     * @return this
     */
    public FindOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    @Override
    public FindOptions collection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Override
    @Nullable
    @MorphiaInternal
    public String collection() {
        return collection;
    }

    /**
     * Sets the comment to log with the query
     *
     * @param comment the comment
     * @return this
     */
    public FindOptions comment(BsonValue comment) {
        this.comment = comment;
        return this;
    }

    /**
     * @return a copy of this instance
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public FindOptions copy() {
        return new FindOptions().copy(this);
    }

    /**
     * Creates an copy of the given options
     *
     * @param original the original to copy
     * @return the new copy
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public FindOptions copy(FindOptions original) {
        this.allowDiskUse = original.allowDiskUse;
        this.batchSize = original.batchSize;
        this.collection = original.collection;
        this.limit = original.limit;
        this.maxTimeMS = original.maxTimeMS;
        this.maxAwaitTimeMS = original.maxAwaitTimeMS;
        this.skip = original.skip;
        this.sort = original.sort;
        this.cursorType = original.cursorType;
        this.noCursorTimeout = original.noCursorTimeout;
        this.partial = original.partial;
        this.collation = original.collation;
        this.comment = original.comment;
        this.hint = original.hint;
        this.hintString = original.hintString;
        this.max = original.max;
        this.min = original.min;
        this.returnKey = original.returnKey;
        this.showRecordId = original.showRecordId;
        this.readConcern = original.readConcern;
        this.readPreference = original.readPreference;
        this.projection = original.projection;
        this.queryLogId = original.queryLogId;

        return this;
    }

    /**
     * Sets the cursor type
     *
     * @param cursorType the type
     * @return this
     */
    public FindOptions cursorType(CursorType cursorType) {
        this.cursorType = Assertions.notNull("cursorType", cursorType);
        return this;
    }

    /**
     * @param disable
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public void disableValidation(boolean disable) {
        this.disableValidation = disable;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public int hashCode() {
        return Objects.hash(allowDiskUse, batchSize, limit, maxTimeMS, maxAwaitTimeMS, skip, sort, cursorType, noCursorTimeout,
                partial, collation, comment, hint, hintString, max, min, returnKey, showRecordId, readConcern, readPreference, projection,
                queryLogId);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindOptions)) {
            return false;
        }
        FindOptions that = (FindOptions) o;
        return batchSize == that.batchSize && limit == that.limit && maxTimeMS == that.maxTimeMS && maxAwaitTimeMS == that.maxAwaitTimeMS
                && skip == that.skip && noCursorTimeout == that.noCursorTimeout
                && partial == that.partial
                && returnKey == that.returnKey && showRecordId == that.showRecordId && Objects.equals(allowDiskUse, that.allowDiskUse)
                && Objects.equals(sort, that.sort) && cursorType == that.cursorType && Objects.equals(collation, that.collation)
                && Objects.equals(comment, that.comment) && Objects.equals(hint, that.hint) && Objects.equals(hintString, that.hintString)
                && Objects.equals(max, that.max) && Objects.equals(min, that.min) && Objects.equals(readConcern, that.readConcern)
                && Objects.equals(readPreference, that.readPreference) && Objects.equals(projection, that.projection)
                && Objects.equals(queryLogId, that.queryLogId);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String toString() {
        return new StringJoiner(", ", FindOptions.class.getSimpleName() + "[", "]")
                .add("allowDiskUse=" + allowDiskUse)
                .add("batchSize=" + batchSize)
                .add("limit=" + limit)
                .add("maxTimeMS=" + maxTimeMS)
                .add("maxAwaitTimeMS=" + maxAwaitTimeMS)
                .add("skip=" + skip)
                .add("sort=" + sort)
                .add("cursorType=" + cursorType)
                .add("noCursorTimeout=" + noCursorTimeout)
                .add("partial=" + partial)
                .add("collation=" + collation)
                .add("comment='" + comment + "'")
                .add("hint=" + hint)
                .add("max=" + max)
                .add("min=" + min)
                .add("returnKey=" + returnKey)
                .add("showRecordId=" + showRecordId)
                .add("readPreference=" + readPreference)
                .add("queryLogId='" + queryLogId + "'")
                .add("projection=" + projection)
                .toString();
    }

    /**
     * Sets the index hint
     *
     * @param hint the hint
     * @return this
     */
    public FindOptions hint(Document hint) {
        this.hint = new Document(hint);
        return this;
    }

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public FindOptions hint(String hint) {
        hintString(hint);
        return this;
    }

    /**
     * Defines the index hint value
     *
     * @param hint the hint
     * @return this
     */
    public FindOptions hintString(String hint) {
        this.hintString = hint;
        return this;
    }

    /**
     * Add top-level variables to the operation. A null value means no variables are set.
     *
     * <p>
     * Allows for improved command readability by separating the variables from the query text.
     * </p>
     *
     * @param variables for find operation or null
     * @return this
     * @since 2.3
     */
    public FindOptions let(Document variables) {
        this.variables = new Document(variables);
        return this;
    }

    /**
     * Sets the limit
     *
     * @param limit the limit
     * @return this
     */
    public FindOptions limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the max index value
     *
     * @param max the max
     * @return this
     */
    public FindOptions max(Document max) {
        this.max = new Document(max);
        return this;
    }

    /**
     * Sets the max await time
     *
     * @param maxAwaitTime the max
     * @param timeUnit     the unit
     * @return this
     */
    public FindOptions maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        Assertions.isTrueArgument("maxAwaitTime > = 0", maxAwaitTime >= 0L);
        this.maxAwaitTimeMS = TimeUnit.MILLISECONDS.convert(maxAwaitTime, timeUnit);
        return this;
    }

    /**
     * Sets the max time
     *
     * @param maxTime  the max
     * @param timeUnit the unit
     * @return this
     */
    public FindOptions maxTime(long maxTime, TimeUnit timeUnit) {
        Assertions.notNull("timeUnit", timeUnit);
        Assertions.isTrueArgument("maxTime > = 0", maxTime >= 0L);
        this.maxTimeMS = TimeUnit.MILLISECONDS.convert(maxTime, timeUnit);
        return this;
    }

    /**
     * Sets the min index value
     *
     * @param min the min
     * @return this
     */
    public FindOptions min(Document min) {
        this.min = new Document(min);
        return this;
    }

    /**
     * Sets whether to disable cursor time out
     *
     * @param noCursorTimeout true if the time should be disabled
     * @return this
     */
    public FindOptions noCursorTimeout(boolean noCursorTimeout) {
        this.noCursorTimeout = noCursorTimeout;
        return this;
    }

    /**
     * Users should not set this under normal circumstances.
     *
     * @param oplogReplay if oplog replay is enabled
     * @return this
     * @deprecated removed from the driver
     */
    @Deprecated(since = "3.0")
    public FindOptions oplogReplay(boolean oplogReplay) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
     *
     * @param partial if partial results for sharded clusters is enabled
     * @return this
     */
    public FindOptions partial(boolean partial) {
        this.partial = partial;
        return this;
    }

    /**
     * @return the query log id used for retrieving the logged query
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String queryLogId() {
        return queryLogId;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public ReadConcern readConcern() {
        return readConcern;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public ReadPreference readPreference() {
        return readPreference;
    }

    @Override
    public FindOptions readConcern(ReadConcern readConcern) {
        this.readConcern = readConcern;
        return this;
    }

    @Override
    public FindOptions readPreference(ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    /**
     * Sets if only the key value should be returned
     *
     * @param returnKey true if only the key should be returned
     * @return this
     */
    public FindOptions returnKey(boolean returnKey) {
        this.returnKey = returnKey;
        return this;
    }

    /**
     * Sets if the record ID should be returned
     *
     * @param showRecordId true if the record id should be returned
     * @return this
     */
    public FindOptions showRecordId(boolean showRecordId) {
        this.showRecordId = showRecordId;
        return this;
    }

    /**
     * Sets how many documents to skip
     *
     * @param skip the count
     * @return this
     */
    public FindOptions skip(int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * Sets to the sort to use
     *
     * @param meta the meta data to sort by
     * @return this
     * @since 2.0
     */
    public FindOptions sort(Meta meta) {
        projection().project(meta);
        return sort(meta.toDatabase());
    }

    /**
     * @return the projection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
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
    public FindOptions sort(Document sort) {
        this.sort = new Document(sort);
        return this;
    }

    /**
     * Sets to the sort to use
     *
     * @param meta  the meta data to sort by
     * @param sorts additional sort elements
     * @return this
     * @since 2.4
     */
    public FindOptions sort(Meta meta, Sort... sorts) {
        projection().project(meta);
        sort(meta.toDatabase());
        for (Sort sort : sorts) {
            this.sort.append(sort.getField(), sort.getOrder());
        }

        return this;
    }

    /**
     * Sets to the sort to use
     *
     * @param sorts the sorts to apply
     * @return this
     */
    public FindOptions sort(Sort... sorts) {
        this.sort = new Document();
        for (Sort sort : sorts) {
            this.sort.append(sort.getField(), sort.getOrder());
        }
        return this;
    }

    public TimeoutMode timeoutMode() {
        return timeoutMode;
    }

    public FindOptions timeoutMode(TimeoutMode timeoutMode) {
        this.timeoutMode = timeoutMode;
        return this;
    }

}
