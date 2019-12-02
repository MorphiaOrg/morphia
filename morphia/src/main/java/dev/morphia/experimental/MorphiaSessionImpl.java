package dev.morphia.experimental;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import dev.morphia.DeleteOptions;
import dev.morphia.IndexHelper;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.QueryFactory;

import java.util.List;

/**
 * @morphia.experimental
 * @morphia.internal
 */
public class MorphiaSessionImpl extends BaseMorphiaSession {

    /**
     * Creates a new session.
     *
     * @param session the client session
     */
    public MorphiaSessionImpl(final ClientSession session, final MongoClient mongoClient, final MongoDatabase database,
                              final Mapper mapper, final IndexHelper indexHelper,
                              final QueryFactory queryFactory) {
        super(session, database, mongoClient, mapper, indexHelper, queryFactory);
    }

    @Override
    public <T> DeleteResult delete(final T entity, final DeleteOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T merge(final T entity, final InsertOneOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> save(final List<T> entities, final InsertManyOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T save(final T entity, final InsertOneOptions options) {
        return super.save(entity, new InsertOneOptions(options)
                  .clientSession(getSession()));
    }

    @Override
    public <T> void insert(final T entity, final InsertOneOptions options) {
        super.insert(entity, new InsertOneOptions(options)
                                 .clientSession(getSession()));
    }

    @Override
    public <T> void insert(final List<T> entities, final InsertManyOptions options) {
        throw new UnsupportedOperationException();
    }
}
