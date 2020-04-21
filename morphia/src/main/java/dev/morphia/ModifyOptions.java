package dev.morphia;

import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import dev.morphia.internal.SessionConfigurable;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Defines the options for a "find and modify" operation.
 *
 * @since 2.0
 */
public class ModifyOptions extends FindOneAndUpdateOptions implements SessionConfigurable<ModifyOptions> {
    private WriteConcern writeConcern;
    private ClientSession clientSession;

    /**
     * Applies the options to the collection
     *
     * @param collection the collection to update
     * @param <T>        the collection type
     * @return either the passed collection or the updated collection
     */
    public <T> MongoCollection<T> apply(final MongoCollection<T> collection) {
        return writeConcern == null
               ? collection
               : collection.withWriteConcern(writeConcern);
    }

    @Override
    public ModifyOptions clientSession(final ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    /**
     * @return the write concern
     * @deprecated use {@link #writeConcern()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    @Override
    public ModifyOptions projection(final Bson projection) {
        super.projection(projection);
        return this;
    }

    @Override
    public ModifyOptions sort(final Bson sort) {
        super.sort(sort);
        return this;
    }

    @Override
    public ModifyOptions upsert(final boolean upsert) {
        super.upsert(upsert);
        return this;
    }

    @Override
    public ModifyOptions returnDocument(final ReturnDocument returnDocument) {
        super.returnDocument(returnDocument);
        return this;
    }

    @Override
    public ModifyOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public ModifyOptions bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    @Override
    public ModifyOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    @Override
    public ModifyOptions arrayFilters(final List<? extends Bson> arrayFilters) {
        super.arrayFilters(arrayFilters);
        return this;
    }

    /**
     * @param writeConcern the write concern
     * @return this
     */
    public ModifyOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @return the write concern to use
     */
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
