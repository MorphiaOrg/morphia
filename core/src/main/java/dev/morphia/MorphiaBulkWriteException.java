package dev.morphia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.WriteConcernError;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Thrown when a list insert or save ({@link Datastore#insert(List, InsertManyOptions)},
 * {@link Datastore#save(List, InsertManyOptions)}) stores some entities but not others.
 * <p>
 * {@link #getFailedEntities()} are the entities that were rejected, and {@link #getUnprocessedEntities()} are those not
 * written because the write stopped at a failure. Together they are exactly the entities that weren't stored, and they can
 * be retried: ids that Morphia generated for them are cleared so a retry inserts them again. Indexes, here and in
 * {@link #getWriteErrors()}, refer to positions in the list that was passed in.
 * <p>
 * Inside a transaction, any write error aborts the transaction, so nothing is stored unless the transaction is retried
 * and commits. List writes in a transaction stop at the first failure even when unordered; the rest are reported as
 * unprocessed, though the server may already have attempted some of them before the transaction aborted.
 * <p>
 * If a list write later fails with an error that isn't about individual entities (a network error, say), that error is
 * thrown instead, with a {@code MorphiaBulkWriteException} for the failures recorded so far attached as a suppressed
 * exception.
 * <p>
 * When the only failures are version conflicts, no server reported an error, and {@link #getServerAddress()} returns the
 * driver's default address.
 *
 * @see InsertManyOptions#ordered(boolean)
 * @since 3.0
 */
public class MorphiaBulkWriteException extends MongoBulkWriteException {
    private final String message;

    private final List<BulkWriteFailure> failures;

    private final List<?> unprocessed;

    /**
     * Creates the exception.
     *
     * @param message           the message
     * @param failures          the rejected entities
     * @param unprocessed       the entities never sent, in the order of the caller's list
     * @param writeResult       the combined result of every write that was sent
     * @param writeConcernError the first write concern error, if any
     * @param serverAddress     the address of the server that reported the first failure
     * @param errorLabels       the error labels of every failed write
     * @param cause             the first non-server failure, if any
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MorphiaBulkWriteException(String message, List<BulkWriteFailure> failures, List<?> unprocessed,
            BulkWriteResult writeResult, @Nullable WriteConcernError writeConcernError, ServerAddress serverAddress,
            Set<String> errorLabels, @Nullable Throwable cause) {
        super(writeResult, writeErrors(failures), writeConcernError, serverAddress, errorLabels);
        this.message = message;
        this.failures = sorted(failures);
        this.unprocessed = List.copyOf(unprocessed);
        if (cause != null) {
            initCause(cause);
        }
    }

    private static List<BulkWriteFailure> sorted(List<BulkWriteFailure> failures) {
        List<BulkWriteFailure> list = new ArrayList<>(failures);
        list.sort(Comparator.comparingInt(BulkWriteFailure::index));
        return List.copyOf(list);
    }

    private static List<BulkWriteError> writeErrors(List<BulkWriteFailure> failures) {
        List<BulkWriteError> errors = new ArrayList<>();
        for (BulkWriteFailure failure : sorted(failures)) {
            WriteError error = failure.error();
            if (error != null) {
                errors.add(new BulkWriteError(error.getCode(), error.getMessage(), error.getDetails(), failure.index()));
            }
        }
        return errors;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @return the rejected entities with their errors, ordered by their position in the caller's list
     */
    public List<BulkWriteFailure> getFailures() {
        return failures;
    }

    /**
     * @param <T> the entity type
     * @return the rejected entities, ordered by their position in the caller's list
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getFailedEntities() {
        return (List<T>) failures.stream()
                .map(BulkWriteFailure::entity)
                .toList();
    }

    /**
     * @param <T> the entity type
     * @return the entities not written because the write stopped at a failure, ordered by their position in the caller's list.
     *         Always empty for unordered writes outside a transaction.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getUnprocessedEntities() {
        return (List<T>) unprocessed;
    }
}
