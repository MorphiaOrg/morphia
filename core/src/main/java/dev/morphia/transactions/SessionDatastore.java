package dev.morphia.transactions;

import java.util.List;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ServerAddress;
import com.mongodb.TransactionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.TransactionBody;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.TimeoutContext;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import com.mongodb.session.ServerSession;

import dev.morphia.DeleteOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.MorphiaDatastore;
import dev.morphia.ReplaceOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.CountOptions;
import dev.morphia.query.FindAndDeleteOptions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;

/**
 * @hidden
 * @since 2.0
 * @morphia.internal
 */
@MorphiaInternal
public class SessionDatastore extends MorphiaDatastore implements MorphiaSession {

    private final ClientSession session;

    private TimeoutContext timeoutContext;

    /**
     * Creates a new session.
     *
     * @param datastore the datastore
     * @param session   the client session
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SessionDatastore(MorphiaDatastore datastore, ClientSession session) {
        super(datastore);
        operations(new TransactionalOperations());
        this.session = session;
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
    public TransactionOptions getTransactionOptions() {
        return session.getTransactionOptions();
    }

    @Override
    public void startTransaction() {
        session.startTransaction();
    }

    @Override
    public void startTransaction(TransactionOptions transactionOptions) {
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
    public <T> T withTransaction(TransactionBody<T> transactionBody) {
        return session.withTransaction(transactionBody);
    }

    @Override
    public BsonTimestamp getSnapshotTimestamp() {
        return session.getSnapshotTimestamp();
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
    public void setTransactionContext(ServerAddress serverAddress, Object o) {
        session.setTransactionContext(serverAddress, o);
    }

    @Override
    public void clearTransactionContext() {
        session.clearTransactionContext();
    }

    @Override
    @Nullable
    public BsonDocument getRecoveryToken() {
        return session.getRecoveryToken();
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
    public void advanceOperationTime(BsonTimestamp operationTime) {
        session.advanceOperationTime(operationTime);
    }

    @Override
    public void advanceClusterTime(BsonDocument clusterTime) {
        session.advanceClusterTime(clusterTime);
    }

    private class TransactionalOperations extends DatastoreOperations {
        @Override
        public <T> long countDocuments(MongoCollection<T> collection, Document query, CountOptions options) {
            return collection.countDocuments(session, query, options);
        }

        @Override
        public <T> DeleteResult deleteMany(MongoCollection<T> collection, Document queryDocument, DeleteOptions options) {
            return collection.deleteMany(session, queryDocument, options);
        }

        @Override
        public <T> DeleteResult deleteOne(MongoCollection<T> collection, Document queryDocument, DeleteOptions options) {
            return collection.deleteOne(session, queryDocument, options);
        }

        @Override
        public <E> FindIterable<E> find(MongoCollection<E> collection, Document query) {
            return collection.find(session, query);
        }

        @Override
        public <T> T findOneAndDelete(MongoCollection<T> mongoCollection, Document queryDocument, FindAndDeleteOptions options) {
            return mongoCollection.findOneAndDelete(session, queryDocument, options);
        }

        @Override
        public <T> T findOneAndUpdate(MongoCollection<T> collection, Document query, Document update, ModifyOptions options) {
            return collection.findOneAndUpdate(session, query, update, options);
        }

        @Override
        public <T> InsertManyResult insertMany(MongoCollection<T> collection, List<T> list, InsertManyOptions options) {
            return collection.insertMany(session, list, options.driver());
        }

        @Override
        public <T> InsertOneResult insertOne(MongoCollection<T> collection, T entity, InsertOneOptions options) {
            return collection.insertOne(session, entity, options.driver());
        }

        @Override
        public <T> UpdateResult replaceOne(MongoCollection<T> collection, T entity, Document filter, ReplaceOptions options) {
            return collection.replaceOne(session, filter, entity, options);
        }

        @Override
        public Document runCommand(Document command) {
            return getMongoClient()
                    .getDatabase("admin")
                    .runCommand(session, command);
        }

        @Override
        public <T> UpdateResult updateMany(MongoCollection<T> collection, Document query, Document updates,
                UpdateOptions options) {
            return collection.updateMany(session, query, updates, options);
        }

        @Override
        public <T> UpdateResult updateMany(MongoCollection<T> collection, Document query, List<Document> updates,
                UpdateOptions options) {
            return collection.updateMany(session, query, updates, options);
        }

        @Override
        public <T> UpdateResult updateOne(MongoCollection<T> collection, Document query, Document updates,
                UpdateOptions options) {
            return collection.updateOne(session, query, updates, options);
        }

        @Override
        public <T> UpdateResult updateOne(MongoCollection<T> collection, Document query, List<Document> updates,
                UpdateOptions options) {
            return collection.updateOne(session, query, updates, options);
        }
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
    public TimeoutContext getTimeoutContext() {
        return timeoutContext;
    }

    public void setTimeoutContext(TimeoutContext timeoutContext) {
        this.timeoutContext = timeoutContext;
    }

    @Override
    public void close() {
        session.close();
    }
}
