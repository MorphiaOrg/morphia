package dev.morphia;

import com.mongodb.WriteConcern;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.WriteConfigurable;

import org.bson.BsonValue;

/**
 * Options related to insertion of documents into MongoDB. The setter methods return {@code this} so that a chaining style can be used.
 *
 * @since 1.3
 */
public class InsertManyOptions implements WriteConfigurable<InsertManyOptions>, CollectionConfigurable<InsertManyOptions> {
    private com.mongodb.client.model.InsertManyOptions options = new com.mongodb.client.model.InsertManyOptions();
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    private String collection;

    /**
     * Creates a new options wrapper
     */
    public InsertManyOptions() {
    }

    /**
     * @param that the options to copy
     * @morphia.internal
     * @hidden
     * @since 2.0
     */
    @MorphiaInternal
    public InsertManyOptions(InsertManyOptions that) {
        this.options = that.options;
        this.writeConcern = that.writeConcern;
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
     * Gets whether to bypass document validation, or null if unspecified. The default is null.
     *
     * @return whether to bypass document validation, or null if unspecified.
     * @mongodb.server.release 3.2
     */
    @Nullable
    public Boolean bypassDocumentValidation() {
        return options.getBypassDocumentValidation();
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.InsertManyOptions#comment(String)
     * @since 2.3
     */
    public InsertManyOptions comment(String comment) {
        options.comment(comment);
        return this;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.InsertManyOptions#comment(BsonValue)
     * @since 2.3
     */
    public InsertManyOptions comment(BsonValue comment) {
        options.comment(comment);
        return this;
    }

    /**
     * Gets whether to bypass document validation, or null if unspecified. The default is null.
     *
     * @return whether to bypass document validation, or null if unspecified.
     * @mongodb.server.release 3.2
     */
    @Nullable
    @Deprecated(forRemoval = true, since = "2.3")
    public Boolean getBypassDocumentValidation() {
        return options.getBypassDocumentValidation();
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
     * @return the driver version of this instance
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public com.mongodb.client.model.InsertManyOptions getOptions() {
        return options;
    }

    /**
     * Gets whether the documents should be inserted in the order provided, stopping on the first failed insertion. The default is true.
     * If false, the server will attempt to insert all the documents regardless of an failures.
     *
     * @return whether the the documents should be inserted in order
     */
    @Deprecated(forRemoval = true, since = "2.3")
    public boolean isOrdered() {
        return options.isOrdered();
    }

    /**
     * @return the driver version of this instance
     * @morphia.internal
     * @hidden
     * @since 2.3
     */
    @MorphiaInternal
    public com.mongodb.client.model.InsertManyOptions options() {
        return options;
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
