package dev.morphia.aggregation.experimental;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import dev.morphia.internal.SessionConfigurable;

import java.util.concurrent.TimeUnit;

public class AggregationOptions implements SessionConfigurable<AggregationOptions> {
    private boolean allowDiskUse;
    private int batchSize;
    private boolean bypassDocumentValidation;
    private Collation collation;
    private long maxTimeMS;
    private ClientSession clientSession;
    private ReadPreference readPreference;
    private ReadConcern readConcern;
    private WriteConcern writeConcern;

    public boolean allowDiskUse() {
        return allowDiskUse;
    }

    public <T> MongoCollection<T> apply(final MongoCollection<T> collection) {
        MongoCollection<T> bound = collection;
        if(readConcern != null) {
            bound = bound.withReadConcern(readConcern);
        }
        if(readPreference != null) {
            bound = bound.withReadPreference(readPreference);
        }

        return bound;
    }

    public boolean getAllowDiskUse() {
        return allowDiskUse;
    }

    public AggregationOptions allowDiskUse(final boolean allowDiskUse) {
        this.allowDiskUse = allowDiskUse;
        return this;
    }

    public int batchSize() {
        return batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public AggregationOptions batchSize(final int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public boolean bypassDocumentValidation() {
        return bypassDocumentValidation;
    }

    public AggregationOptions bypassDocumentValidation(final boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = bypassDocumentValidation;
        return this;
    }

    @Override
    public AggregationOptions clientSession(final ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    public Collation collation() {
        return collation;
    }

    public AggregationOptions collation(final Collation collation) {
        this.collation = collation;
        return this;
    }

    public boolean getBypassDocumentValidation() {
        return bypassDocumentValidation;
    }

    public Collation getCollation() {
        return collation;
    }

    public long getMaxTime(TimeUnit unit) {
        return unit.convert(maxTimeMS, TimeUnit.MILLISECONDS);
    }

    public long getMaxTimeMS() {
        return maxTimeMS;
    }

    public long maxTimeMS() {
        return maxTimeMS;
    }

    public AggregationOptions maxTimeMS(final long maxTimeMS) {
        this.maxTimeMS = maxTimeMS;
        return this;
    }

    public ReadPreference readPreference() {
        return readPreference;
    }

    public ReadPreference getReadPreference() {
        return readPreference;
    }

    public AggregationOptions readPreference(final ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    public ReadConcern readConcern() {
        return readConcern;
    }

    public ReadConcern getReadConcern() {
        return readConcern;
    }

    public AggregationOptions readConcern(final ReadConcern readConcern) {
        this.readConcern = readConcern;
        return this;
    }

    public WriteConcern writeConcern() {
        return writeConcern;
    }

    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    public AggregationOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }
}
