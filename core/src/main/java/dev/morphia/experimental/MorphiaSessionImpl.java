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
    public MorphiaSessionImpl(ClientSession session, MongoClient mongoClient, MongoDatabase database,
                              Mapper mapper, QueryFactory queryFactory) {
        super(session, mongoClient, database, mapper, queryFactory);
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
    public <T> DeleteResult delete(T entity, DeleteOptions options) {
        return super.delete(entity, new DeleteOptions(options)
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
    public <T> T save(T entity, InsertOneOptions options) {
        return super.save(entity, new InsertOneOptions(options)
                                      .clientSession(findSession(options)));
    }
}
