package dev.morphia.experimental;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import dev.morphia.DeleteOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.QueryFactory;

import java.util.List;

/**
 * @morphia.experimental
 * @morphia.internal
 * @since 2.0
 */
public class MorphiaSessionImpl extends BaseMorphiaSession {

    /**
     * Creates a new session.
     *
     * @param session      the client session
     * @param database     the database
     * @param mapper       the mapper
     * @param mongoClient  the client
     * @param queryFactory the factory
     */
    public MorphiaSessionImpl(final ClientSession session, final MongoClient mongoClient, final MongoDatabase database,
                              final Mapper mapper, final QueryFactory queryFactory) {
        super(session, mongoClient, database, mapper, queryFactory);
    }

    @Override
    public <T> void insert(final T entity, final InsertOneOptions options) {
        super.insert(entity, new InsertOneOptions(options)
                                 .clientSession(findSession(options)));
    }

    @Override
    public <T> void insert(final List<T> entities, final InsertManyOptions options) {
        super.insert(entities, new InsertManyOptions(options)
                                   .clientSession(findSession(options)));
    }

    @Override
    public <T> DeleteResult delete(final T entity, final DeleteOptions options) {
        return super.delete(entity, new DeleteOptions(options)
                                        .clientSession(findSession(options)));
    }

    @Override
    public <T> T merge(final T entity, final InsertOneOptions options) {
        return super.merge(entity, new InsertOneOptions(options)
                                       .clientSession(findSession(options)));
    }

    @Override
    public <T> List<T> save(final List<T> entities, final InsertManyOptions options) {
        return super.save(entities, new InsertManyOptions(options)
                                        .clientSession(findSession(options)));
    }

    @Override
    public <T> T save(final T entity, final InsertOneOptions options) {
        return super.save(entity, new InsertOneOptions(options)
                                      .clientSession(findSession(options)));
    }
}
