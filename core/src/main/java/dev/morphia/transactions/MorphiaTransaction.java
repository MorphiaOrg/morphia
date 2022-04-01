package dev.morphia.transactions;

/**
 * Defines the functional interface for executing statements within a transaction.  Care should be take to use only the session reference
 * passed rather than any direct reference to another Datastore.
 *
 * @param <T> the entity type
 */
public interface MorphiaTransaction<T> {
    /**
     * Executes the transaction body
     *
     * @param session the session to use
     * @return any result from the transaction
     */
    T execute(MorphiaSession session);
}
