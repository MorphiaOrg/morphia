package dev.morphia.query;

import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

/**
 * Defines options to use for find and delete operations
 */
public class FindAndDeleteOptions extends FindOneAndDeleteOptions {
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;

    /**
     * @return the write concern to use
     */
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public FindAndDeleteOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    @Override
    public FindAndDeleteOptions projection(final Bson projection) {
        super.projection(projection);
        return this;
    }

    @Override
    public FindAndDeleteOptions sort(final Bson sort) {
        super.sort(sort);
        return this;
    }

    @Override
    public FindAndDeleteOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public FindAndDeleteOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * @param sort the sort to apply
     * @return this
     */
    public FindAndDeleteOptions sort(final Document sort) {
        super.sort(sort);
        return this;
    }
}
