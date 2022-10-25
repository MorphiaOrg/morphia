package dev.morphia;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.lang.Nullable;

import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.WriteConfigurable;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Defines the options for a "find and modify" operation.
 *
 * @since 2.0
 */
public class ModifyOptions extends FindOneAndUpdateOptions implements WriteConfigurable<ModifyOptions>,
        CollectionConfigurable<ModifyOptions> {
    private WriteConcern writeConcern;
    private String collection;

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions collection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String collection() {
        return collection;
    }

    /**
     * @param hint the hint to apply
     * @return this
     * @see #hint(Bson)
     * @since 2.2
     */
    public ModifyOptions hint(Document hint) {
        super.hint(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions projection(@Nullable Bson projection) {
        super.projection(projection);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions sort(@Nullable Bson sort) {
        super.sort(sort);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions upsert(boolean upsert) {
        super.upsert(upsert);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions returnDocument(ReturnDocument returnDocument) {
        super.returnDocument(returnDocument);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions maxTime(long maxTime, TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions bypassDocumentValidation(@Nullable Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions collation(@Nullable Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyOptions arrayFilters(@Nullable List<? extends Bson> arrayFilters) {
        super.arrayFilters(arrayFilters);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @see FindOneAndUpdateOptions#hint(Bson)
     * @since 2.2
     */
    public ModifyOptions hint(@Nullable Bson hint) {
        super.hint(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @see FindOneAndUpdateOptions#hintString(String)
     * @since 2.2
     */
    public ModifyOptions hintString(@Nullable String hint) {
        super.hintString(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.3
     */
    @Override
    public ModifyOptions comment(String comment) {
        super.comment(comment);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.3
     */
    @Override
    public ModifyOptions comment(BsonValue comment) {
        super.comment(comment);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.3
     */
    @Override
    public ModifyOptions let(Bson variables) {
        super.let(variables);
        return this;
    }

    /**
     * @param writeConcern the write concern
     * @return this
     */
    public ModifyOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @return the write concern to use
     */
    @Nullable
    public WriteConcern writeConcern() {
        return writeConcern;
    }
}
