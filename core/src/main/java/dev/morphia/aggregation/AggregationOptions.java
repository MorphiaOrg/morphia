package dev.morphia.aggregation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;

import dev.morphia.DatastoreImpl;
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
    private String collection;
    private boolean allowDiskUse;
    private Integer batchSize;
    private boolean bypassDocumentValidation;
    private Collation collation;
    private Long maxTimeMS;
    private ReadPreference readPreference;
    private ReadConcern readConcern;
    private WriteConcern writeConcern;
    private Document hint;

    /**
     * @return the configuration value
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public boolean allowDiskUse() {
        return allowDiskUse;
    }

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
     * Applies the configured options to the collection.
     *
     * @param <T>        the collection type
     * @param <S>        the result type
     * @param pipeline   the stage pipeline
     * @param datastore
     * @param collection the collection to configure
     * @param resultType the result type
     * @return the updated collection
     * @morphia.internal
     */
    @MorphiaInternal
    <S, T> AggregateIterable<T> apply(List<Document> pipeline,
            DatastoreImpl datastore, MongoCollection<T> collection, Class<S> resultType) {
        MongoDatabase database = datastore.getDatabase();
        MongoCollection<T> bound = prepare(collection, database);
        if (readConcern != null) {
            bound = bound.withReadConcern(readConcern);
        }
        if (readPreference != null) {
            bound = bound.withReadPreference(readPreference);
        }
        AggregateIterable<T> aggregate = datastore.operations().aggregate(bound, pipeline, resultType);
        aggregate
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
        if (hint != null) {
            aggregate.hint(hint);
        }

        return aggregate;
    }

    /**
     * @param unit the target unit type
     * @return the configuration value
     */
    public long maxTime(TimeUnit unit) {
        return unit.convert(maxTimeMS, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the configuration value
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public int batchSize() {
        return batchSize;
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
     * @return the configuration value
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public boolean bypassDocumentValidation() {
        return bypassDocumentValidation;
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
     * @return the configuration value
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public Collation collation() {
        return collation;
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
     * @return
     * @since 2.3
     */
    @Override
    public AggregationOptions collection(String collection) {
        this.collection = collection;
        return this;
    }

    @Override
    public String collection() {
        return collection;
    }

    /**
     * @return the hint for which index to use. A null value means no hint is set.
     * @mongodb.server.release 3.6
     * @since 2.0
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public Document hint() {
        return hint;
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    public AggregationOptions hint(String hint) {
        this.hint = new Document("hint", hint);
        return this;
    }

    /**
     * @return the configuration value
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public long maxTimeMS() {
        return maxTimeMS;
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
     * @return the configuration value
     */
    public ReadConcern readConcern() {
        return readConcern;
    }

    /**
     * @return the configuration value
     */
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

    @Override
    public String toString() {
        return new StringBuilder("AggregationOptions{")
                .append("allowDiskUse=").append(allowDiskUse)
                .append(", batchSize=").append(batchSize)
                .append(", bypassDocumentValidation=").append(bypassDocumentValidation)
                .append(", collation=").append(collation)
                .append(", maxTimeMS=").append(maxTimeMS)
                .append(", readPreference=").append(readPreference)
                .append(", readConcern=").append(readConcern)
                .append(", writeConcern=").append(writeConcern)
                .append(", hint=").append(hint)
                .append('}')
                .toString();
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
     */
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
