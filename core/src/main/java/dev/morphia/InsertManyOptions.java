package dev.morphia;

import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.lang.Nullable;
import dev.morphia.internal.SessionConfigurable;
import dev.morphia.internal.WriteConfigurable;

/**
 * Options related to insertion of documents into MongoDB.  The setter methods return {@code this} so that a chaining style can be used.
 *
 * @since 1.3
 */
public class InsertManyOptions implements SessionConfigurable<InsertManyOptions>, WriteConfigurable<InsertManyOptions>,
                                              AlternateCollection<InsertManyOptions> {
    private com.mongodb.client.model.InsertManyOptions options = new com.mongodb.client.model.InsertManyOptions();
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    private ClientSession clientSession;
    private String collection;

    /**
     * Creates a new options wrapper
     */
    public InsertManyOptions() {
    }

    /**
     * @param that the options to copy
     * @morphia.internal
     * @since 2.0
     */
    public InsertManyOptions(InsertManyOptions that) {
        this.options = that.options;
        this.writeConcern = that.writeConcern;
        this.clientSession = that.clientSession;
        this.collection = that.collection;
    }

    /**
     * Sets whether to bypass document validation.
     *
     * @param bypassDocumentValidation whether to bypass document validation, or null if unspecified
     * @return this
     * @mongodb.server.release 3.2
     */
    public InsertManyOptions bypassDocumentValidation(Boolean bypassDocumentValidation) {
        options.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    /**
     * Set the client session to use for the insert.
     *
     * @param clientSession the client session
     * @return this
     */
    public InsertManyOptions clientSession(@Nullable ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    /**
     * The client session to use for the insertion.
     *
     * @return the client session
     */
    public ClientSession clientSession() {
        return clientSession;
    }

    @Override
    public InsertManyOptions collection(@Nullable String collection) {
        this.collection = collection;
        return this;
    }

    @Override
    @Nullable
    public String collection() {
        return collection;
    }

    /**
     * Gets whether to bypass document validation, or null if unspecified.  The default is null.
     *
     * @return whether to bypass document validation, or null if unspecified.
     * @mongodb.server.release 3.2
     */
    @Nullable
    public Boolean getBypassDocumentValidation() {
        return options.getBypassDocumentValidation();
    }

    /**
     * @return the driver version of this instance
     */
    public com.mongodb.client.model.InsertManyOptions getOptions() {
        return options;
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    @Override
    public InsertManyOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    @Override
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * Gets whether the documents should be inserted in the order provided, stopping on the first failed insertion. The default is true.
     * If false, the server will attempt to insert all the documents regardless of an failures.
     *
     * @return whether the the documents should be inserted in order
     */
    public boolean isOrdered() {
        return options.isOrdered();
    }

    /**
     * Sets whether the server should insert the documents in the order provided.
     *
     * @param ordered true if documents should be inserted in order
     * @return this
     */
    public InsertManyOptions ordered(boolean ordered) {
        options.ordered(ordered);
        return this;
    }
}
