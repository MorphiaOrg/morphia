package dev.morphia.query;

import java.util.concurrent.TimeUnit;

import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.lang.Nullable;

import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.WriteConfigurable;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Defines options to use for find and delete operations
 */
public class FindAndDeleteOptions extends FindOneAndDeleteOptions implements WriteConfigurable<FindAndDeleteOptions>,
        CollectionConfigurable<FindAndDeleteOptions> {
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    private String collection;

    @Override
    public FindAndDeleteOptions collection(String collection) {
        this.collection = collection;
        return this;
    }

    @Override
    public String collection() {
        return collection;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.3
     */
    @Override
    public FindAndDeleteOptions comment(String comment) {
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
    public FindAndDeleteOptions comment(BsonValue comment) {
        super.comment(comment);
        return this;
    }

    /**
     * @param hint the hint to apply
     * @return this
     * @see #hint(Bson)
     * @since 2.2
     */
    public FindAndDeleteOptions hint(Document hint) {
        super.hint(hint);
        return this;
    }

    @Override
    public FindAndDeleteOptions projection(@Nullable Bson projection) {
        super.projection(projection);
        return this;
    }

    @Override
    public FindAndDeleteOptions sort(@Nullable Bson sort) {
        super.sort(sort);
        return this;
    }

    @Override
    public FindAndDeleteOptions maxTime(long maxTime, TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public FindAndDeleteOptions collation(@Nullable Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public FindAndDeleteOptions hint(@Nullable Bson hint) {
        super.hint(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public FindAndDeleteOptions hintString(@Nullable String hint) {
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
    public FindAndDeleteOptions let(Bson variables) {
        super.let(variables);
        return this;
    }

    /**
     * @param sort the sort to apply
     * @return this
     */
    public FindAndDeleteOptions sort(Document sort) {
        super.sort(sort);
        return this;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public FindAndDeleteOptions writeConcern(@Nullable WriteConcern writeConcern) {
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
