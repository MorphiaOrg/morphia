package dev.morphia.aggregation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.cursor.TimeoutMode;
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.ReadConfigurable;
import dev.morphia.internal.WriteConfigurable;

import org.bson.Document;

/**
 * Defines options to be applied to an aggregation pipeline.
 */
@SuppressWarnings("unused")
public class AggregationOptions implements ReadConfigurable<AggregationOptions>, WriteConfigurable<AggregationOptions>,
        CollectionConfigurable<AggregationOptions> {
    private boolean allowDiskUse;
    private Integer batchSize;
    private boolean bypassDocumentValidation;
    private Collation collation;
    private String collection;
    private String comment;
    private Document hint;
    private Document let;
    private Long maxAwaitTime;
    private Long maxTimeMS;
    private ReadConcern readConcern;
    private ReadPreference readPreference;
    private TimeoutMode timeoutMode;
    private WriteConcern writeConcern;

    /**
     * Enables writing to temporary files.
     *
     * @param allowDiskUse true to enable
     * @return this
     */
    public AggregationOptions allowDiskUse(boolean allowDiskUse) {
        this.allowDiskUse = allowDiskUse;
        return this;
    }

    /**
     * Sets the batch size for fetching results.
     *
     * @param batchSize the size
     * @return this
     */
    public AggregationOptions batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Enables the aggregation to bypass document validation during the operation. This lets you insert documents that do not
     * meet the validation requirements.
     * <p>
     * Applicable only if you specify the $out or $merge aggregation stages.
     *
     * @param bypassDocumentValidation true to enable the bypass
     * @return this
     */
    public AggregationOptions bypassDocumentValidation(boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = bypassDocumentValidation;
        return this;
    }

    /**
     * Specifies the collation to use for the operation.
     * <p>
     * Collation allows users to specify language-specific rules for string comparison, such as rules for letter case and accent marks.
     *
     * @param collation the collation to use
     * @return this
     */
    public AggregationOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    /**
     * Specify an alternate collection to aggregate from rather than the collection mapped to the type used to create the aggregation
     * initially.
     *
     * @param collection the name of the collection to use
     * @return this
     * @since 2.3
     */
    @Override
    public AggregationOptions collection(@Nullable String collection) {
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
     * Sets the comment for this operation. A null value means no comment is set.
     *
     * @param comment the comment
     * @return this
     * @since 3.0
     * @mongodb.server.release 3.6
     */
    public AggregationOptions comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this
     * @mongodb.server.release 3.6
     */
    public AggregationOptions hint(String hint) {
        return hint(new Document("hint", hint));
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this
     * @mongodb.server.release 3.6
     * @since 3.0
     */
    public AggregationOptions hint(Document hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Defines any variables to use when evaluating the pipeline
     * 
     * @param let the variable definitions
     * @return this
     * @since 3.0
     */
    public AggregationOptions let(Document let) {
        this.let = let;
        return this;
    }

    /**
     * Specifies a time limit in milliseconds for processing operations on a cursor. If you do not specify a value for maxTimeMS,
     * operations will not time out. A value of 0 explicitly specifies the default unbounded behavior.
     *
     * @param maxTimeMS the max time in milliseconds
     * @return this
     */
    public AggregationOptions maxTimeMS(long maxTimeMS) {
        this.maxTimeMS = maxTimeMS;
        return this;
    }

    /**
     * Specifies a time limit for processing operations on a cursor. If you do not specify a value for maxTime,
     * operations will not time out. A value of 0 explicitly specifies the default unbounded behavior.
     *
     * @param maxTime  the max time
     * @param timeUnit the time unit
     * @return this
     * @since 3.0
     */
    public AggregationOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTimeMS = timeUnit.toMillis(maxTime);
        return this;
    }

    /**
     * Specifies a time limit for processing operations on a cursor. If you do not specify a value for maxTime,
     * operations will not time out. A value of 0 explicitly specifies the default unbounded behavior.
     *
     * @param maxTime  the max time
     * @param timeUnit the time unit
     * @return this
     * @since 3.0
     */
    public AggregationOptions maxAwaitTime(long maxTime, TimeUnit timeUnit) {
        this.maxAwaitTime = timeUnit.toMillis(maxTime);
        return this;
    }

    /**
     * @return the configuration value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReadConcern readConcern() {
        return readConcern;
    }

    /**
     * @return the configuration value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReadPreference readPreference() {
        return readPreference;
    }

    /**
     * Specifies the read concern.
     *
     * @param readConcern the read concern to use
     * @return this
     */
    public AggregationOptions readConcern(ReadConcern readConcern) {
        this.readConcern = readConcern;
        return this;
    }

    /**
     * Sets the read preference to use
     *
     * @param readPreference the read preference
     * @return this
     */
    public AggregationOptions readPreference(ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    /**
     * Sets the timeout mode
     *
     * @param timeoutMode the timeout mode
     * @return this
     * @since 3.0
     */
    public AggregationOptions timeoutMode(TimeoutMode timeoutMode) {
        this.timeoutMode = timeoutMode;
        return this;
    }

    /**
     * @return the timeout mode
     * @hidden
     * @morphia.internal
     * @since 3.0
     */
    @MorphiaInternal
    public TimeoutMode timeoutMode() {
        return timeoutMode;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Override
    @MorphiaInternal
    public String toString() {
        return ("AggregationOptions{allowDiskUse=%s, batchSize=%d, bypassDocumentValidation=%s, collation=%s, maxTimeMS=%d, " +
                "readPreference=%s, readConcern=%s, writeConcern=%s, hint=%s}").formatted(allowDiskUse, batchSize,
                        bypassDocumentValidation, collation, maxTimeMS, readPreference, readConcern, writeConcern, hint);
    }

    /**
     * Sets the write concern to use
     *
     * @param writeConcern the write concern
     * @return this
     */
    public AggregationOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @return the configuration value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * Applies the configured options to the collection.
     *
     * @param <T>        the collection type
     * @param <S>        the result type
     * @param documents  the stage documents
     * @param database
     * @param collection the collection to configure
     * @param resultType the result type
     * @return the updated collection
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    <S, T> AggregateIterable<S> apply(List<Document> documents,
            MongoDatabase database, MongoCollection<T> collection, Class<S> resultType) {
        MongoCollection<T> bound = prepare(collection, database);
        if (readConcern != null) {
            bound = bound.withReadConcern(readConcern);
        }
        if (readPreference != null) {
            bound = bound.withReadPreference(readPreference);
        }
        AggregateIterable<S> aggregate = bound.aggregate(documents, resultType)
                .allowDiskUse(allowDiskUse)
                .bypassDocumentValidation(bypassDocumentValidation);
        if (batchSize != null) {
            aggregate.batchSize(batchSize);
        }
        if (collation != null) {
            aggregate.collation(collation);
        }
        if (maxTimeMS != null) {
            aggregate.maxTime(maxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
        if (maxAwaitTime != null) {
            aggregate.maxAwaitTime(maxAwaitTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
        if (hint != null) {
            aggregate.hint(hint);
        }
        if (let != null) {
            aggregate.let(let);
        }
        if (comment != null) {
            aggregate.comment(comment);
        }

        return aggregate;
    }

    /**
     * @param unit the target unit type
     * @return the configuration value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public long maxTime(TimeUnit unit) {
        return unit.convert(maxTimeMS, TimeUnit.MILLISECONDS);
    }

    /**
     * @param unit the target unit type
     * @return the configuration value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public long maxAwaitTime(TimeUnit unit) {
        return unit.convert(maxAwaitTime, TimeUnit.MILLISECONDS);
    }
}
