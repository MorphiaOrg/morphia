package dev.morphia.internal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.WriteConcernError;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;

import dev.morphia.BulkWriteFailure;
import dev.morphia.MorphiaBulkWriteException;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

/**
 * Collects the outcome of one list insert or save, in terms of positions in the caller's list, and builds the
 * {@link MorphiaBulkWriteException} if anything wasn't stored.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class BulkWriteFailures {
    private final int total;

    private final List<BulkWriteFailure> failures = new ArrayList<>();

    private final List<Unprocessed> unprocessed = new ArrayList<>();

    private final Set<String> errorLabels = new LinkedHashSet<>();

    private int inserted;

    private int matched;

    private int modified;

    private boolean unacknowledged;

    @Nullable
    private WriteConcernError writeConcernError;

    @Nullable
    private ServerAddress serverAddress;

    @Nullable
    private RuntimeException cause;

    /**
     * @param total the number of entities in the caller's list
     */
    public BulkWriteFailures(int total) {
        this.total = total;
    }

    /**
     * Records a batch that was stored in full.
     *
     * @param count the number of documents inserted
     */
    public void recordInserted(int count) {
        inserted += count;
    }

    /**
     * Records one entity saved individually.
     *
     * @param result the replace's result, or null if the entity was inserted
     */
    public void recordSaved(@Nullable UpdateResult result) {
        if (result == null) {
            inserted++;
        } else if (!result.wasAcknowledged()) {
            unacknowledged = true;
        } else {
            matched += (int) result.getMatchedCount();
            modified += (int) result.getModifiedCount();
        }
    }

    /**
     * Records a batch the server rejected some documents of.
     *
     * @param e             the driver's exception
     * @param groupEntities the batch, in the order it was sent
     * @param groupIndexes  each batch entity's position in the caller's list
     */
    public void recordBulkFailure(MongoBulkWriteException e, List<?> groupEntities, int[] groupIndexes) {
        for (BulkWriteError error : e.getWriteErrors()) {
            failures.add(new BulkWriteFailure(groupEntities.get(error.getIndex()), groupIndexes[error.getIndex()], error, null));
        }
        BulkWriteResult result = e.getWriteResult();
        if (result.wasAcknowledged()) {
            inserted += result.getInsertedCount();
        } else {
            unacknowledged = true;
        }
        if (writeConcernError == null) {
            writeConcernError = e.getWriteConcernError();
        }
        server(e.getServerAddress());
        errorLabels.addAll(e.getErrorLabels());
    }

    /**
     * Records an individually saved entity the server rejected.
     *
     * @param entity the entity
     * @param index  its position in the caller's list
     * @param e      the driver's exception
     */
    public void recordWriteFailure(Object entity, int index, MongoWriteException e) {
        failures.add(new BulkWriteFailure(entity, index, e.getError(), null));
        server(e.getServerAddress());
        errorLabels.addAll(e.getErrorLabels());
    }

    /**
     * Records an entity that failed without a server write error, such as a version conflict.
     *
     * @param entity the entity
     * @param index  its position in the caller's list
     * @param e      the failure
     */
    public void recordException(Object entity, int index, RuntimeException e) {
        failures.add(new BulkWriteFailure(entity, index, null, e));
        if (cause == null) {
            cause = e;
        }
    }

    /**
     * Records an entity that was never sent.
     *
     * @param entity the entity
     * @param index  its position in the caller's list
     */
    public void recordUnprocessed(Object entity, int index) {
        unprocessed.add(new Unprocessed(entity, index));
    }

    /**
     * @return true if any entity was rejected or never sent
     */
    public boolean hasFailures() {
        return !failures.isEmpty() || !unprocessed.isEmpty();
    }

    /**
     * @return the positions of every entity that was rejected or never sent
     */
    public BitSet notStored() {
        BitSet bits = new BitSet(total);
        failures.forEach(f -> bits.set(f.index()));
        unprocessed.forEach(u -> bits.set(u.index()));
        return bits;
    }

    /**
     * Throws a {@link MorphiaBulkWriteException} describing what wasn't stored, if anything wasn't.
     */
    public void throwIfFailed() {
        if (!hasFailures()) {
            return;
        }
        List<Object> notSent = unprocessed.stream()
                .sorted(Comparator.comparingInt(Unprocessed::index))
                .map(Unprocessed::entity)
                .toList();
        long conflicts = failures.stream().filter(f -> f.exception() != null).count();
        String message = Sofia.bulkWriteFailed(failures.size() + unprocessed.size(), total, failures.size(), unprocessed.size(),
                conflicts);
        BulkWriteResult result = unacknowledged
                ? BulkWriteResult.unacknowledged()
                : BulkWriteResult.acknowledged(inserted, matched, 0, modified, List.of(), List.of());
        throw new MorphiaBulkWriteException(message, failures, notSent, result, writeConcernError,
                serverAddress != null ? serverAddress : new ServerAddress(), errorLabels, cause);
    }

    private void server(@Nullable ServerAddress address) {
        if (serverAddress == null) {
            serverAddress = address;
        }
    }

    private record Unprocessed(Object entity, int index) {
    }
}
