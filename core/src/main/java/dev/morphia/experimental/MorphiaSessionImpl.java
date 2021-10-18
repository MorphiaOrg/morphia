package dev.morphia.experimental;

import com.mongodb.client.ClientSession;
import com.mongodb.client.result.DeleteResult;
import dev.morphia.DatastoreImpl;
import dev.morphia.DeleteOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;

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
     * @param datastore the datastore
     * @param session   the client session
     */
    public MorphiaSessionImpl(DatastoreImpl datastore, ClientSession session) {
        super(datastore, session);
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
