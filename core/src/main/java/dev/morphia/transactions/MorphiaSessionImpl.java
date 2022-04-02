package dev.morphia.transactions;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ServerAddress;
import com.mongodb.TransactionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.TransactionBody;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import com.mongodb.session.ServerSession;
import dev.morphia.DatastoreImpl;
import dev.morphia.DeleteOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.annotations.internal.MorphiaInternal;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;

import java.util.List;

/**
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class MorphiaSessionImpl extends DatastoreImpl implements MorphiaSession {

    private final ClientSession session;

    /**
     * Creates a new session.
     *
     * @param datastore the datastore
     * @param session   the client session
     */
    public MorphiaSessionImpl(DatastoreImpl datastore, ClientSession session) {
        super(datastore);
        this.session = session;
    }

    @Override
    public <T> DeleteResult delete(T entity, DeleteOptions options) {
        return super.delete(entity, new DeleteOptions(options)
            .clientSession(findSession(options)));
    }

    @Override
    public <T> void insert(T entity, InsertOneOptions options) {
        super.insert(entity, new InsertOneOptions(options)
            .clientSession(findSession(options)));
    }

    @Override
    public <T> void insert(List<T> entities, InsertManyOptions options) {
        super.insert(entities, new InsertManyOptions(options)
            .clientSession(findSession(options)));
    }

    @Override
    public <T> T merge(T entity, InsertOneOptions options) {
        return super.merge(entity, new InsertOneOptions(options)
            .clientSession(findSession(options)));
    }

    @Override
    public <T> List<T> save(List<T> entities, InsertManyOptions options) {
        return super.save(entities, new InsertManyOptions(options)
            .clientSession(findSession(options)));
    }

    @Override
    public void commitTransaction() {
        session.commitTransaction();
    }

    @Override
    public <T> T save(T entity, InsertOneOptions options) {
        return super.save(entity, new InsertOneOptions(options)
            .clientSession(findSession(options)));
    }

    @Override
    @Nullable
    public ServerAddress getPinnedServerAddress() {
        return session.getPinnedServerAddress();
    }

    @Override
    public boolean hasActiveTransaction() {
        return session.hasActiveTransaction();
    }

    @Override
    public boolean notifyMessageSent() {
        return session.notifyMessageSent();
    }

    @Override
    public void notifyOperationInitiated(Object operation) {
        session.notifyOperationInitiated(operation);
    }

    @Override
    @Nullable
    public BsonDocument getRecoveryToken() {
        return session.getRecoveryToken();
    }

    @Override
    public TransactionOptions getTransactionOptions() {
        return session.getTransactionOptions();
    }

    @Override
    public void startTransaction() {
        session.startTransaction();
    }

    @Override
    public BsonTimestamp getSnapshotTimestamp() {
        return session.getSnapshotTimestamp();
    }

    @Override
    public void startTransaction(TransactionOptions transactionOptions) {
        session.startTransaction(transactionOptions);
    }

    @Override
    public void abortTransaction() {
        session.abortTransaction();
    }

    @Override
    public <T> T withTransaction(TransactionBody<T> transactionBody) {
        return session.withTransaction(transactionBody);
    }

    @Override
    public <T> T withTransaction(TransactionBody<T> transactionBody, TransactionOptions options) {
        return session.withTransaction(transactionBody, options);
    }

    /**
     * @return the session
     */
    @NonNull
    public ClientSession getSession() {
        return session;
    }

    @Override
    public Object getTransactionContext() {
        return session.getTransactionContext();
    }

    @Override
    public boolean isCausallyConsistent() {
        return session.isCausallyConsistent();
    }

    @Override
    public void setTransactionContext(ServerAddress serverAddress, Object o) {
        session.setTransactionContext(serverAddress, o);
    }

    @Override
    public void clearTransactionContext() {
        session.clearTransactionContext();
    }

    @Override
    public void setRecoveryToken(BsonDocument recoveryToken) {
        session.setRecoveryToken(recoveryToken);
    }

    @Override
    public ClientSessionOptions getOptions() {
        return session.getOptions();
    }

    @Override
    public Object getOriginator() {
        return session.getOriginator();
    }

    @Override
    public ServerSession getServerSession() {
        return session.getServerSession();
    }

    @Override
    public BsonTimestamp getOperationTime() {
        return session.getOperationTime();
    }

    @Override
    public void advanceOperationTime(BsonTimestamp operationTime) {
        session.advanceOperationTime(operationTime);
    }

    @Override
    public void advanceClusterTime(BsonDocument clusterTime) {
        session.advanceClusterTime(clusterTime);
    }

    @Override
    public void setSnapshotTimestamp(BsonTimestamp bsonTimestamp) {
        session.setSnapshotTimestamp(bsonTimestamp);
    }

    @Override
    public BsonDocument getClusterTime() {
        return session.getClusterTime();
    }

    @Override
    public void close() {
        session.close();
    }
}
