package dev.morphia.experimental;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ServerAddress;
import com.mongodb.TransactionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.TransactionBody;
import com.mongodb.lang.Nullable;
import com.mongodb.session.ServerSession;
import dev.morphia.DatastoreImpl;
import dev.morphia.IndexHelper;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.QueryFactory;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;

/**
 * @morphia.internal
 * @morphia.experimental
 */
public abstract class BaseMorphiaSession extends DatastoreImpl implements MorphiaSession {
    private ClientSession session;

    BaseMorphiaSession(final ClientSession session, final MongoDatabase database,
                       final MongoClient mongoClient, final Mapper mapper, final IndexHelper indexHelper,
                       final QueryFactory queryFactory) {
        super(database, indexHelper, mongoClient, mapper, queryFactory);
        this.session = session;
    }

    public ClientSession getSession() {
        return session;
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
    public TransactionOptions getTransactionOptions() {
        return session.getTransactionOptions();
    }

    @Override
    public void startTransaction() {
        session.startTransaction();
    }

    @Override
    public void startTransaction(final TransactionOptions transactionOptions) {
        session.startTransaction(transactionOptions);
    }

    @Override
    public void commitTransaction() {
        session.commitTransaction();
    }

    @Override
    public void abortTransaction() {
        session.abortTransaction();
    }

    @Override
    public ClientSessionOptions getOptions() {
        return session.getOptions();
    }

    @Override
    public boolean isCausallyConsistent() {
        return session.isCausallyConsistent();
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
    public void advanceOperationTime(final BsonTimestamp operationTime) {
        session.advanceOperationTime(operationTime);
    }

    @Override
    public void advanceClusterTime(final BsonDocument clusterTime) {
        session.advanceClusterTime(clusterTime);
    }

    @Override
    public BsonDocument getClusterTime() {
        return session.getClusterTime();
    }

    @Override
    public void close() {
        session.close();
    }

    @Override
    @Nullable
    public ServerAddress getPinnedServerAddress() {
        return session.getPinnedServerAddress();
    }

    @Override
    public void setPinnedServerAddress(final ServerAddress address) {
        session.setPinnedServerAddress(address);
    }

    @Override
    public <T> T withTransaction(final TransactionBody<T> transactionBody) {
        return session.withTransaction(transactionBody);
    }

    @Override
    public <T> T withTransaction(final TransactionBody<T> transactionBody, final TransactionOptions options) {
        return session.withTransaction(transactionBody, options);
    }

    @Override
    @Nullable
    public BsonDocument getRecoveryToken() {
        return session.getRecoveryToken();
    }

    @Override
    public void setRecoveryToken(final BsonDocument recoveryToken) {
        session.setRecoveryToken(recoveryToken);
    }
}
